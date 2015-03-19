import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;

/**
 * @author: LiyueWang
 * @time: 2015/03/04
 * @function: calculate the page contribution for big datasets, using SparseMatrix
 *  
 */

public class PageContV2 {
	public static int ITERATIONS=100;
	public static double DAMPLE_FACTOR=0.85;
	public static double CONVERGENCE_THRESH=0.000001;
	
	public static void main(String[] args) throws Exception
	{
		long begin=System.currentTimeMillis();	
		long mbegin=Runtime.getRuntime().totalMemory()-Runtime.getRuntime().freeMemory();
		
		String datafile="E:\\MyDropbox\\Dropbox\\Study\\SFU\\SFU-CourseStudy\\2014Fall-726-A3\\ASN\\project\\testbenchmark\\com-amazon.ungraph0.05.small.reindex.txt";	
		String outfile="E:\\MyDropbox\\Dropbox\\Study\\SFU\\SFU-CourseStudy\\2014Fall-726-A3\\ASN\\project\\testbenchmark\\com-amazon.ungraph0.05.small.reindex_sparse_";				
		boolean weighted=false;	
		
		/*
		if(args.length!=2)
		{
			System.out.println("Usage: PageContV2 infile weighted ");
			return;
		}
		
		String datafile=args[0];
		String outfile=datafile.substring(0,datafile.length()-4)+"_sparse_";
		boolean weighted=false;
		if(args[1]=="true")
			weighted=true;
		*/
		
		/**
		 * 
		 */
		SparseMatrix adj_matrix=init_adj_matrix(datafile,weighted);		
		SparseMatrix tran_matrix=init_tran_matrix(adj_matrix);
		double[] pageRank=compute_pageRank_thresh(tran_matrix);
		SparseMatrix pathContribution=compute_pathcont_thresh(tran_matrix);	
		SparseMatrix pageContribution=compute_pagecont(pageRank,pathContribution);
		SparseMatrix simi_matrix=compute_similarity(pageContribution);
			
//		adj_matrix.save_to_file(outfile+"adjMatrix.txt");
//		tran_matrix.save_to_file(outfile+"tranMatrix.txt");
//		PageRankClustering.save_pr(pageRank,outfile+"pageRank.txt");
//		pathContribution.save_to_file(outfile+"pathCont.txt");
//		pageContribution.save_to_file(outfile+"pageCont.txt");
//		simi_matrix.save_to_file(outfile+"simiMatrix.txt");
		/**
		 * Approxmation Version
		 */
//		SparseMatrix adj_matrix=init_adj_matrix(datafile,weighted);	
//		SparseMatrix tran_matrix=init_tran_matrix(adj_matrix);
//		double[] pageRank=compute_pageRank_thresh(tran_matrix);
//		double[][] pathContributionV1=approx_pathcont_matrix(adj_matrix,1-DAMPLE_FACTOR,0.01,1.0);
//		SparseMatrix pathContribution=SparseMatrix.create_from_2d_array(pathContributionV1);
//		SparseMatrix pageContribution=compute_pagecont(pageRank,pathContribution);
//		SparseMatrix simi_matrix=compute_similarity(pageContribution);
		
//		PageRankClustering.save_to_file(pathContribution,outfile+"Approx_pathCont.txt");
//		SparseMatrix pathContribution=approx_pathcont(adj_matrix,1-DAMPLE_FACTOR,0.01,1.0);	
//		pathContribution.save_to_file(outfile+"Approx_SM_pathCont.txt");
					
		/**
		 * Temporary test
		 */
//		Compare();
		
		//test memory
		long mend=Runtime.getRuntime().totalMemory()-Runtime.getRuntime().freeMemory();
		long memory_cost=mend-mbegin;
		System.out.println("Memory cost: "+memory_cost+" bytes.");
		
		//test time
		long end=System.currentTimeMillis();
		long time_cost=end-begin; //unit: ms
		System.out.println("Time cost: "+Long.toString(time_cost)+"ms");
	}
	//compare the result between approximation pathcont and pathcont
	public static void Compare()throws IOException
	{
		String datafile="E:\\MyDropbox\\Dropbox\\Study\\SFU\\SFU-CourseStudy\\2014Fall-726-A3\\ASN\\project\\testbenchmark\\com-amazon.ungraph0.05.small.reindex.txt";	
//		String datafile="E:\\MyDropbox\\Dropbox\\Study\\SFU\\SFU-CourseStudy\\2014Fall-726-A3\\ASN\\project\\testbenchmark\\graph9.txt";	
//		String outfile="E:\\MyDropbox\\Dropbox\\Study\\SFU\\SFU-CourseStudy\\2014Fall-726-A3\\ASN\\project\\testbenchmark\\com-amazon.ungraph0.05.small.reindex_sparse_";				
		boolean weighted=false;	
		SparseMatrix adj_matrix=init_adj_matrix(datafile,weighted);	
		double alpha=1-DAMPLE_FACTOR;
		
		for(int i=0;i<15;i++)
			approx_pathcont(i,adj_matrix,alpha,0.01,15);
	}
	/* if graph with weight, adj_matrix element is weight, otherwise 1
	 * adj_matrix[i][j]=1 if exits edge(j,i)
	 *    datafile: firstline: Node: 34
	 *    datafile of unweighted graph form: node1 node2 //only one space between node1 node2
	 *    datafile of weighted graph form: node1 node2 weight 
	 * the node is index from 0
	 */
	public static SparseMatrix init_adj_matrix(String datafile, boolean weighted) throws IOException
	{
		BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(datafile)));
		String line=null;	
		//read the # of nodes from the first line: Nodes: xxx
		String firstline=reader.readLine();
		int nodes_num=Integer.parseInt(firstline.split("\\s")[1]);
		
		LinkedList<SparseMatrixEntry>[] rows = new LinkedList[nodes_num];
		LinkedList<SparseMatrixEntry>[] cols = new LinkedList[nodes_num];
		for(int i = 0; i < nodes_num; i++)
			rows[i] = new LinkedList<SparseMatrixEntry>();
		for(int i = 0; i < nodes_num; i++)
			cols[i] = new LinkedList<SparseMatrixEntry>(); 

		SparseMatrix matrix = new SparseMatrix(rows, cols);
		
		setConThresh(nodes_num);
		System.out.println("\ninitial adjacency matrix(SparseMatrix) of directed graph......");
		while ((line = reader.readLine()) != null) {
			int sindex=Integer.parseInt(line.split("\\t")[0]);   
			int tindex=Integer.parseInt(line.split("\\t")[1]);	
			if(weighted)
			{
				double value=Double.parseDouble(line.split(" ")[2]);
				matrix.rows[tindex].add( new SparseMatrixEntry(value, sindex) ); 
				matrix.cols[sindex].add( new SparseMatrixEntry(value, tindex) );
			}
			else
			{
				double value=1.0;
				matrix.rows[tindex].add( new SparseMatrixEntry(value, sindex) );
				matrix.cols[sindex].add( new SparseMatrixEntry(value, tindex) );
			}
		}
		matrix.keep_sort();
		System.out.println("initial adjacenct matrix successfully!");
		reader.close();
		
		return matrix;
	}
		
	//transfer the adj_matrix to tran_matrix, tran_matrix[i][j]=adj_matrix[i][j]/outdegree(j)
	public static SparseMatrix init_tran_matrix(SparseMatrix adj_matrix) throws IOException
	{
		int nodes_num=adj_matrix.n_rows;
		LinkedList<SparseMatrixEntry>[] rows = new LinkedList[nodes_num];
		LinkedList<SparseMatrixEntry>[] cols = new LinkedList[nodes_num];
		for(int i = 0; i < nodes_num; i++)
			rows[i] = new LinkedList<SparseMatrixEntry>();
		for(int i = 0; i < nodes_num; i++)
			cols[i] = new LinkedList<SparseMatrixEntry>(); 
		SparseMatrix matrix = new SparseMatrix(rows, cols);
		
		System.out.println("\ninitial transition matrix(SparseMatrix) of directed graph......");
		for(int i=0;i<nodes_num;i++)
		{
			for(SparseMatrixEntry entry: adj_matrix.rows[i])
			{
				double value=entry.value/adj_matrix.cols[entry.index].size();
				matrix.rows[i].add( new SparseMatrixEntry(value, entry.index) );
				matrix.cols[entry.index].add( new SparseMatrixEntry(value, i) );
			}
		}
		System.out.println("initial transition matrix successfully!");
		
		return matrix;
	}
	
	//compute pagerank with CONVERGENCE_THRESH
	public static double[] compute_pageRank_thresh(SparseMatrix tran_matrix)
	{
		int n=tran_matrix.n_cols;
		double[] prv=new double[n]; //page rank vector
		double[] new_prv=new double[n];
		for(int i=0;i<n;i++)
		{
			prv[i]=1.0/n;
		}
		int it=0;
		for(it=0;;it++)
		{
			for(int i=0;i<n;i++)
			{
				new_prv[i]=0;
				for(Iterator iter=tran_matrix.rows[i].iterator();iter.hasNext();)
				{
					SparseMatrixEntry entry=(SparseMatrixEntry)iter.next();
					new_prv[i]+=(entry.value)*prv[entry.index];
				}
				//new_prv[i]=new_prv[i]*DAMPLE_FACTOR+(1-DAMPLE_FACTOR)/n;
				new_prv[i]=new_prv[i]*DAMPLE_FACTOR+(1-DAMPLE_FACTOR); //No divide n
			}
			
			if(check_for_convergence(new_prv, prv) == true)
				break;
			System.arraycopy(new_prv,0,prv,0,n);			
		}
		System.out.println("\n pagerank_vector iteration#="+it+" for CONVERGENCE_THRESH="+CONVERGENCE_THRESH);	
		return prv;
	}

	public static SparseMatrix compute_pathcont_thresh(SparseMatrix tran_matrix)throws Exception 
	{
		int nodes_num=tran_matrix.n_cols;
		/* M=(1-dample_factor)*sum(dample_factor^t*L^t) t:0->infinity  (L:trans_matrix)
         **** when t=0, dample_factor^t*L^t=identity_matrix, when t=1: dample_factor^t*L^t=dample_factor*L
		 **** when t=1, sum=identity_matrix+dample_factor*L
		*/
		SparseMatrix pathcm=SparseMatrix.create_identity_matrix(nodes_num, nodes_num);
		SparseMatrix tran_matrix_power=new SparseMatrix(tran_matrix); //TODO: check reference?
		
		double dample_power=DAMPLE_FACTOR;
		boolean converged = true;
		int it=1;
		for(it=1;;it++)
		{
			converged = true;
			pathcm=SparseMatrix.matrix_add(pathcm, SparseMatrix.matrix_multiply_scalar(tran_matrix_power,dample_power));
			SparseMatrix new_tran_matrix_power=SparseMatrix.matrix_multiply(tran_matrix_power,tran_matrix);
			if(check_for_convergence(new_tran_matrix_power,tran_matrix_power,dample_power))	break;
			tran_matrix_power=new_tran_matrix_power;
			dample_power*=DAMPLE_FACTOR;
		}
		pathcm.multiply_scalar(1-DAMPLE_FACTOR);
		
		System.out.println("\n pathcm iteration#="+it+" for CONVERGENCE_THRESH="+CONVERGENCE_THRESH);			
		return pathcm;
	}
	
	//for row i, all nodes's page contribution for node i
	public static SparseMatrix compute_pagecont(double[] prv,SparseMatrix pathcm) throws IOException
	{	
		int nodes_num=pathcm.n_cols;
		LinkedList<SparseMatrixEntry>[] rows = new LinkedList[nodes_num];
		LinkedList<SparseMatrixEntry>[] cols = new LinkedList[nodes_num];
		for(int i = 0; i < nodes_num; i++)
			rows[i] = new LinkedList<SparseMatrixEntry>();
		for(int i = 0; i < nodes_num; i++)
			cols[i] = new LinkedList<SparseMatrixEntry>(); 

		SparseMatrix pagecm = new SparseMatrix(rows, cols);
		
		//pagecm[i][j]=prv[j]*pathcm[i][j]/pathcm[j][j];
		for(int i = 0; i < nodes_num; i++)
			for(Iterator<SparseMatrixEntry> iter = pathcm.rows[i].iterator(); iter.hasNext(); )
			{
				SparseMatrixEntry entry=(SparseMatrixEntry)iter.next();
				int index_j=entry.index;
				double pathcmjj=pathcm.getRowEntry(index_j, index_j).value;
				if(pathcmjj==0.0)
					System.err.println("pathcm[j][j]==0.0!");
				double value=prv[index_j]*(entry.value)/pathcmjj;
				pagecm.rows[i].add(new SparseMatrixEntry(value,index_j));
				pagecm.cols[index_j].add(new SparseMatrixEntry(value,i));
			}

		return pagecm;
	}
	
	//TODO: tricky set CONVERGENCE_THRESH according the number of nodes
	public static void setConThresh(int nodes_num)
	{
		double thresh=0.001;
		while(nodes_num>0)
		{
			nodes_num/=10;
			thresh*=0.1;
		}
		CONVERGENCE_THRESH=thresh;
		System.out.println("set the covergence thresh as="+thresh);
	}
	
	// check whether convergence for page rank computation
	public static boolean check_for_convergence(double[] score, double[] old_score)
	{
		boolean converged = true;

		for(int i = 0; i < score.length; i++)
		{
			double diff = Math.abs(score[i] - old_score[i]) / Math.min(score[i], old_score[i]);
			if(diff >= CONVERGENCE_THRESH)
			{
				converged = false;
				break;
			}
		}
		return converged;
	}
	
	//TODO:tricky:convergence condition
	public static boolean check_for_convergence(SparseMatrix matrix,SparseMatrix old_matrix,double x)
	{
		boolean converged = true;
		for(int i=0;i<matrix.n_rows;i++)
		{
			Iterator<SparseMatrixEntry> it1=matrix.rows[i].iterator();
			Iterator<SparseMatrixEntry> it2=old_matrix.rows[i].iterator();
			while(it1.hasNext()&&it2.hasNext())
			{
				SparseMatrixEntry entry1=(SparseMatrixEntry)it1.next();
				SparseMatrixEntry entry2=(SparseMatrixEntry)it2.next();
				double diff = Math.abs(entry1.value - entry2.value) / Math.min(entry1.value, entry2.value);
				if(diff*x >= CONVERGENCE_THRESH)
				{
					converged = false;
					break;
				}
			}
		}
		
		return converged;
	}
	
	//compute similarity matrix using cosine similarity
	public static SparseMatrix compute_similarity(SparseMatrix pagecm) throws IOException
	{
		int nodes_num=pagecm.n_cols;
		
		LinkedList<SparseMatrixEntry>[] rows = new LinkedList[nodes_num];
		LinkedList<SparseMatrixEntry>[] cols = new LinkedList[nodes_num];
		for(int i = 0; i < nodes_num; i++)
			rows[i] = new LinkedList<SparseMatrixEntry>();
		for(int i = 0; i < nodes_num; i++)
			cols[i] = new LinkedList<SparseMatrixEntry>(); 
		SparseMatrix simi_matrix = new SparseMatrix(rows, cols);
		
		for(int i=0;i<nodes_num;i++)
		{
			simi_matrix.rows[i].add(new SparseMatrixEntry(1.0,i));
			simi_matrix.cols[i].add(new SparseMatrixEntry(1.0,i));
			for(int j=i+1;j<nodes_num;j++)
			{
				double simi=cosine_similarity(pagecm.rows[i],pagecm.rows[j]);
				if(simi>0.0)
				{
					simi_matrix.rows[i].add(new SparseMatrixEntry(simi,j));
					simi_matrix.cols[j].add(new SparseMatrixEntry(simi,i));
				}
			}
		}
		return simi_matrix;
	}
	
	//compute similarity between each pair of nodes
	public static double cosine_similarity(LinkedList<SparseMatrixEntry> vec1,LinkedList<SparseMatrixEntry> vec2)
	{
		double similarity=0.0;
		double dotsum=0.0;
		double sum1=0.0;
		double sum2=0.0;
		
		Iterator iter1 = vec1.iterator();
		Iterator iter2 = vec2.iterator();
		if(!iter1.hasNext() || !iter2.hasNext())
			return similarity;

		SparseMatrixEntry m_entry1 = (SparseMatrixEntry) iter1.next();
		SparseMatrixEntry m_entry2 = (SparseMatrixEntry) iter2.next();
		while(true)
		{
			if(m_entry1.index == m_entry2.index)
			{
				dotsum += m_entry1.value * m_entry2.value;
				sum1 += m_entry1.value * m_entry1.value;
				sum2 += m_entry2.value * m_entry2.value;
				if(!iter1.hasNext() || !iter2.hasNext())
					break;
				m_entry1 = (SparseMatrixEntry)iter1.next();
				m_entry2 = (SparseMatrixEntry)iter2.next();
			}
			else if(m_entry1.index < m_entry2.index)
			{
				sum1 += m_entry1.value * m_entry1.value;
				if(!iter1.hasNext())
					break;
				m_entry1 = (SparseMatrixEntry) iter1.next();
			}
			else //m_entry1.index > m_entry2.index
			{
				sum2 += m_entry2.value * m_entry2.value;
				if(!iter2.hasNext())
					break;
				m_entry2 = (SparseMatrixEntry) iter2.next();
			}
		}
		sum1=Math.pow(sum1, 0.5);
		sum2=Math.pow(sum2, 0.5);
		similarity=dotsum/(sum1*sum2);
		return similarity;
	}

	//An approximation algorithm[By Andersen et al] to compute path contribution vector
	public static double[] approx_pathcont(int v_index,SparseMatrix adj_matirx, double alpha,double theta, double pmax)
	{
		int n=adj_matirx.n_rows;
		double[] p=new double[n];
		Arrays.fill(p,0.0);
		double[] r=new double[n];
		Arrays.fill(r,0.0);
		r[v_index]=1.0;
		
		LinkedList<Integer> queue=new LinkedList<Integer>();
		queue.offer(v_index);
		int cnt=0;
		while(!queue.isEmpty())
		{
			int u_index=queue.poll();
			cnt++;
			//pushback(u)
			p[u_index]+=alpha*r[u_index];
			//TODO: for each w such that w->u
			LinkedList<SparseMatrixEntry> ws=adj_matirx.rows[u_index];
			for(SparseMatrixEntry entry:ws)
			{
				r[entry.index]+=(1-alpha)*r[u_index]/adj_matirx.cols[entry.index].size();
				if(r[entry.index]>theta)	
					queue.offer(entry.index);
			}
			r[u_index]=0;
			if(L1Distance(p)>=pmax)	
			{
				System.out.println("break in >=pmax");
				break;
			}
		}
		System.out.println("for node "+v_index+", # of pushback="+cnt);
		for(int i=0;i<n;i++)
		{
			if(p[i]!=0.0)	
			{
				BigDecimal b=new   BigDecimal(p[i]);
				double value=b.setScale(10, BigDecimal.ROUND_HALF_UP).doubleValue();
				System.out.print(i+":"+value+" ");
			}
		}
		System.out.println();
		return p;
	}
	
	//An approximation algorithm[By Andersen et al] to compute path contribution matrix
	public static double[][] approx_pathcont_matrix(SparseMatrix adj_matirx, double alpha,double theta, double pmax)
	{
		double[][] p=new double[adj_matirx.n_rows][adj_matirx.n_cols];
		double[][] r=new double[adj_matirx.n_rows][adj_matirx.n_cols];
		LinkedList<Integer>[] queue=new LinkedList[adj_matirx.n_rows];
		for(int i=0;i<adj_matirx.n_rows;i++)
		{
			Arrays.fill(p[i],0.0);
			Arrays.fill(r[i],0.0);
			r[i][i]=1.0;
			queue[i]=new LinkedList<Integer>(); 
			queue[i].add(i);
		}
		int[] push_num=new int[adj_matirx.n_rows];
		Arrays.fill(push_num,0);
		
		for(int i=0;i<adj_matirx.n_rows;i++)
		{
			while(!queue[i].isEmpty())
			{
				int u_index=queue[i].poll();
				push_num[i]++;
				//pushback(u)
				p[i][u_index]+=alpha*r[i][u_index];
				//TODO: for each w such that w->u
				LinkedList<SparseMatrixEntry> ws=adj_matirx.rows[u_index];
				for(SparseMatrixEntry entry:ws)
				{
					r[i][entry.index]+=(1-alpha)*r[i][u_index]/adj_matirx.cols[entry.index].size();
					if(r[i][entry.index]>theta)	
						queue[i].offer(entry.index);
				}
				r[i][u_index]=0.0;
//				if(L1Distance(p[i])>=pmax)	
//				{
//					System.out.println("break in p[i]>=pmax");
//					break;
//				}
			}
		}
//		for(int i=0;i<push_num.length;i++)
//			System.out.println("For node "+i+", the push#="+push_num[i]);
		return p;
	}
	
	//Useless: much slower than approx_pathcont_matrix
	public static SparseMatrix approx_pathcont(SparseMatrix adj_matirx, double alpha,double theta, double pmax)
	{
		SparseMatrix p=SparseMatrix.create_zero_matrix(adj_matirx.n_rows,adj_matirx.n_cols);
		SparseMatrix r=SparseMatrix.create_identity_matrix(adj_matirx.n_rows,adj_matirx.n_cols);
		LinkedList<Integer>[] queue=new LinkedList[adj_matirx.n_rows];
		//Arrays.fill(queue,new LinkedList<Integer>());
		for(int i=0;i<adj_matirx.n_rows;i++)
		{
			queue[i]=new LinkedList<Integer>(); 
			queue[i].add(i);
		}
		
		int[] push_num=new int[adj_matirx.n_rows];
		
		for(int i=0;i<adj_matirx.n_rows;i++)
		{
			while(!queue[i].isEmpty())
			{
				int u_index=queue[i].poll();
				push_num[i]++;
				//pushback(u)
				double r_iu=r.getRowEntry(i, u_index).value;
				if(p.getRowEntry(i, u_index)==null)
					p.addEntry(i, u_index, alpha*r_iu);
				else
					p.setEntry(i, u_index, p.getRowEntry(i, u_index).value+alpha*r_iu);
				r.setEntry(i, u_index, 0.0);
				//TODO: for each w such that w->u
				LinkedList<SparseMatrixEntry> ws=adj_matirx.rows[u_index];
				for(SparseMatrixEntry entry:ws)
				{
					double addvalue=(1-alpha)*r_iu/adj_matirx.cols[entry.index].size();
					if(r.getRowEntry(i, entry.index)!=null)
						addvalue+=r.getRowEntry(i, entry.index).value;
					r.setEntry(i,entry.index,addvalue);
					if(addvalue>theta)
						queue[i].offer(entry.index);
				}
//				if(L1Distance(p.rows[i])>=pmax)	
//				{
//					System.out.println("break in p[i]>=pmax");
//					break;
//				}
			}
		}
		for(int i=0;i<push_num.length;i++)
			System.out.println("For node "+i+", the push#="+push_num[i]);
		p.keep_sort();
		return p;
		
	}
		
	//compute L1-Distance of vector p: ||p||1
	public static double L1Distance(double[] p)
	{
		double sum=0.0;
		for(int i=0;i<p.length;i++)
			sum+=p[i];
		return sum;
	}
	//compute L1-Distance of vector p: ||p||1
	public static double L1Distance(LinkedList<SparseMatrixEntry> pi)
	{
		double sum=0.0;
		for(Iterator<SparseMatrixEntry> it=pi.iterator();it.hasNext();)
			sum+=((SparseMatrixEntry)it.next()).value;
		return sum;
	}
}
