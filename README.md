# MSA2.0
MSA2.0��һ��DNA/RNA/Protein���еĵ�����Hadoop�����бȶ������������Java���Կ�������Hadoop�Ĳ��м��㻷����ӵ�нϿ�ıȶ��ٶȡ���ʹ�ü�㣬��ʹ��û��������Hadoop��Ⱥ���������ʹ���䵥�����ʼ���顣�����������ļ��ϴ󣨳���1GB�������ǽ�����������Hadoop��Ⱥ�����У���Լʱ�䡣

Home page: https://github.com/ShixiangWan/MSA2.0

##��������

* Hadoop 2.7.2
* JDK >= 1.8
* Eclipse Mars

##ʹ�÷���
* ������ǵ���ʹ���û���
```
java -jar MSA2.0.jar <input_file> <output_file> <algorithm type>
# ���磺java -jar MSA2.0.jar /home/user/input.txt /home/user/output.txt 0
```
 * input_file��fasta��ʽ�������ļ���λ�ڱ��أ�
 * output_file������ȶԽ���ļ���λ�ڱ��أ�
 * algorithm type�����бȶ��㷨�����͡�0�����׺���ȶ��㷨���ٶ���죬��ֻ�ʺ�DNA/RNA��1�������BLOSUM62�÷־����KBand�ȶ��㷨��ֻ�ʺ�Protein��2������ڷ����϶���ֵ�KBand�ȶ��㷨��ֻ�ʺ�DNA/RNA��3����Trie tree�ȶ��㷨�����ٶȽ�����ֻ�ʺ�DNA/RNA��4����������ƶȾ���ıȶ��㷨���ٶ����������������ƶȼ��������ʹ�ã���ֻ�ʺ�DNA/RNA��

* �������Hadoop��Ⱥ�û���
```
hadoop jar MSA2.0.jar <input_file> <output_file> <dfs_path> <algorithm type>
# ���磺hadoop jar MSA2.0.jar /home/user/input.txt /home/user/output.txt hdfs://hadoop-master:9000/msa 0
```
 * input_file/output_file/algorithm type���뵥��ʹ����ͬ��
 * dfs_path��Hadoop��ȺDFS���ֲ�ʽ�ļ�ϵͳ���д��ʵ���ļ���·����

##������־
* 2016-09-07, version 0.1:
  * ��ɻ������ܡ�