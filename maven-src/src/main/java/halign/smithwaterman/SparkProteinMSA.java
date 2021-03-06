package halign.smithwaterman;

import jaligner.Alignment;
import jaligner.Sequence;
import jaligner.SmithWatermanGotoh;
import jaligner.matrix.MatrixLoader;
import jaligner.util.SequenceParser;
import org.apache.log4j.Logger;
import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.api.java.function.Function;
import org.apache.spark.api.java.function.Function2;
import org.apache.spark.api.java.function.PairFunction;
import org.apache.spark.broadcast.Broadcast;
import scala.Tuple2;
import utils.FormatUtils;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

/**
 * Implementation of Protein Multiple Sequence Alignment based on Smith Waterman Algorithm & Spark API.
 *
 * @author ShixiangWan
 * */
public class SparkProteinMSA {

    private static Logger logger = Logger.getLogger(SparkProteinMSA.class);

    /**
     * Run multiple sequence alignment.
     *
     * @param jsc java spark context, which can be initialized by <b>JavaSparkContext</b>.
     * @param inputFile input file "path+name", fasta format.
     * @param outputFile output file "path+name", fasta format.
     * */
    public void start(JavaSparkContext jsc, String inputFile, String outputFile) {
        logger.info("(Spark mode for Protein) Loading data ... " + inputFile);

        long startTime = System.currentTimeMillis();
        FormatUtils formatUtils = new FormatUtils();
        formatUtils.readFasta(inputFile, false);
        List<String> fastaKeyList = formatUtils.getS_key();
        List<String> fastaValList = formatUtils.getS_val();
        final String firstVal = fastaValList.get(0);
        JavaRDD<String> fastaKeyJavaRDD = jsc.parallelize(fastaKeyList).cache();
        JavaPairRDD<String, String> fastaDataJavaPairRDD = fastaKeyJavaRDD.zip(jsc.parallelize(fastaValList));

        System.out.println((System.currentTimeMillis() - startTime) + "ms");
        fastaKeyJavaRDD = fastaKeyJavaRDD.coalesce(2, true);

        /*generate map: out2 -> out1 */
        logger.info("MultiThread MSA ... ");
        int firstValLen1 = firstVal.length() + 1;
        final Broadcast<String> fastaFirstValBC = jsc.broadcast(firstVal);
        /*key is not changed, but value is calculated as int[] contains spaces*/
        JavaPairRDD<String, int[]> fastaMSADataJavaPairRDD = fastaDataJavaPairRDD.mapToPair(
                (PairFunction<Tuple2<String, String>, String, String>) stringTuple2 -> {
                    Sequence s1 = SequenceParser.parse(fastaFirstValBC.getValue());
                    Sequence s2 = SequenceParser.parse(stringTuple2._2);
                    Alignment alignment = SmithWatermanGotoh.align(s1, s2, MatrixLoader.load("BLOSUM80"), 10f, 0.5f);
                    return new Tuple2<>(new String(alignment.getSequence2()), new String(alignment.getSequence1()));
                }).mapValues(
                (Function<String, int[]>) stringTuple2 -> {
                    int index = 0;
                    int lineSpace[] = new int[firstValLen1];
                    for (char c : stringTuple2.toCharArray()) {
                        if (c == '-') lineSpace[index]++;
                        else index++;
                    }
                    return lineSpace;
                });
        fastaMSADataJavaPairRDD = fastaMSADataJavaPairRDD.coalesce(2, true);
        int firstSpaceArray[] = fastaMSADataJavaPairRDD.values().reduce(
                (Function2<int[], int[], int[]>) (integer, integer2) -> {
                    for (int i = 0; i < integer2.length; i++) {
                        int int2 = integer2[i];
                        if (integer[i] < int2) integer[i] = int2;
                    }
                    return integer;
                });
        String firstVal0 = firstVal;
        /*calculate the first sequence: firstVal0*/
        StringBuilder stringBuilder = new StringBuilder();
        for (int i = 0; i < firstValLen1; i++) {
            for (int j = 0; j < firstSpaceArray[i]; j++) stringBuilder.append('-');
            if (i != firstValLen1 - 1) stringBuilder.append(firstVal0.charAt(i));
        }
        int lastNum = firstSpaceArray[firstSpaceArray.length - 1];
        for (int i = 0; i < lastNum; i++)
            stringBuilder = stringBuilder.append('-');
        firstVal0 = stringBuilder.toString();
        System.out.println((System.currentTimeMillis() - startTime) + "ms");

        /*merge msa out*/
        logger.info("Converting results ... ");
        final String firstValFinal = firstVal0;
        JavaRDD<String> fastaMSAOutJavaRDD = fastaMSADataJavaPairRDD.map(
                (Function<Tuple2<String, int[]>, String>) stringTuple2 -> {
                    String line = stringTuple2._1;
                    int spaces[] = stringTuple2._2;
                    // record spaces' position.
                    int pos = 0;
                    for (int j = 0; j < firstValLen1; j++) {
                        int gap = firstSpaceArray[j] - spaces[j];
                        pos += spaces[j];
                        if (gap > 0) {
                            if (pos < line.length()) {
                                /*normal*/
                                StringBuilder sb = new StringBuilder();
                                for (int k = 0; k < gap; k++) sb.append('-');
                                line = line.substring(0, pos).concat(sb.toString()).concat(line.substring(pos));
                            } else {
                                /*for some sequences, exceed out of array, we need to break it*/
                                if (line.length() > firstValFinal.length()) {
                                    line = line.substring(0, firstValFinal.length());
                                }
                                break;
                            }
                        }
                        pos += gap + 1;
                    }
                    /*for some sequences which can not meet the same length as firstVal, we need add it*/
                    if (line.length() < firstValFinal.length()) {
                        int gap = firstValFinal.length() - line.length();
                        StringBuilder sb = new StringBuilder();
                        for (int k = 0; k < gap; k++) sb.append('-');
                        line = line.concat(sb.toString());
                    }
                    return line;
                });

        System.out.println((System.currentTimeMillis() - startTime) + "ms");

        logger.info("Saving final results ... ");
        List<Tuple2<String, String>> outDataList = fastaKeyJavaRDD.zip(fastaMSAOutJavaRDD).collect();
        try {
            BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(outputFile));
            for (Tuple2 tuple2 : outDataList) {
                bufferedWriter.write(tuple2._1.toString() + "\n");
                bufferedWriter.write(tuple2._2.toString() + "\n");
            }
            bufferedWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println((System.currentTimeMillis() - startTime) + "ms");
    }
}
