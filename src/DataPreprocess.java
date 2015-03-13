import java.io.*;
import java.util.*;


public class DataPreprocess {
	
	public static void main(String[] args)throws Exception
	{
		//java DataPreprocess -1 //args.length==1
		if(args.length==0)
		{
			printHelp();
		}
		else
		{
			long begin=System.currentTimeMillis();
			String model=args[0];
			if(model.equals("-1"))
			{
				String infile=args[1];
				removeDup(infile);
				
				long end=System.currentTimeMillis();
				long time_cost=end-begin; //unit: ms
				System.out.println("Time cost: "+Long.toString(time_cost)+"ms");
			}
			else if(model.equals("-2"))
			{
				String dfile=args[1];
				String efile=args[2];
				double percent=Double.parseDouble(args[3]);
				HashMap<Integer,Integer> nodes_index=DataPreprocess.extraCommunity(dfile,percent);
				DataPreprocess.extraEdges(efile,nodes_index,percent);
				
				long end=System.currentTimeMillis();
				long time_cost=end-begin; //unit: ms
				System.out.println("Time cost: "+Long.toString(time_cost)+"ms");
			}
			else
				printHelp();
		}
	}
	/**
	 * Usage print
	 */
	static void printHelp()
	{
		System.out.println("Usage: DataPreprocess [-option]");
		System.out.println("where options include:");
		System.out.println("	-1	cmtyFile //clean the community file");
		System.out.println("	-2	cmtyFile ungraphFile percent //extra subnetworks according percent");
	}
	
	/**
     * data preprocess
     */
	public static void preprocess() throws IOException
	{
		
		/**
		 * Original data files 
		 */
		//dblp
//		String dfile="E:\\MyDropbox\\Dropbox\\Study\\SFU\\SFU-CourseStudy\\2014Fall-726-A3\\ASN\\project\\DBLP\\com-dblp.top5000.cmty.clean.txt";
//		String efile="E:\\MyDropbox\\Dropbox\\Study\\SFU\\SFU-CourseStudy\\2014Fall-726-A3\\ASN\\project\\DBLP\\com-dblp.ungraph.txt";
		//amazon
		String dfile="E:\\MyDropbox\\Dropbox\\Study\\SFU\\SFU-CourseStudy\\2014Fall-726-A3\\ASN\\project\\AMAZON\\com-amazon.top5000.cmty.clean.txt";
		String efile="E:\\MyDropbox\\Dropbox\\Study\\SFU\\SFU-CourseStudy\\2014Fall-726-A3\\ASN\\project\\AMAZON\\com-amazon.ungraph.txt";
		//youtube
//		String dfile="E:\\MyDropbox\\Dropbox\\Study\\SFU\\SFU-CourseStudy\\2014Fall-726-A3\\ASN\\project\\YOUTUBE\\com-youtube.top5000.cmty.clean.txt";
//		String efile="E:\\MyDropbox\\Dropbox\\Study\\SFU\\SFU-CourseStudy\\2014Fall-726-A3\\ASN\\project\\YOUTUBE\\com-youtube.ungraph.txt";
		//live
//		String dfile="E:\\MyDropbox\\Dropbox\\Study\\SFU\\SFU-CourseStudy\\2014Fall-726-A3\\ASN\\project\\LIVE\\com-lj.top5000.cmty.clean.txt";
//		String efile="E:\\MyDropbox\\Dropbox\\Study\\SFU\\SFU-CourseStudy\\2014Fall-726-A3\\ASN\\project\\LIVE\\com-lj.ungraph.txt";
		//ORKUT
//		String dfile="E:\\MyDropbox\\Dropbox\\Study\\SFU\\SFU-CourseStudy\\2014Fall-726-A3\\ASN\\project\\ORKUT\\com-orkut.top5000.cmty.clean.txt";
//		String efile="E:\\MyDropbox\\Dropbox\\Study\\SFU\\SFU-CourseStudy\\2014Fall-726-A3\\ASN\\project\\ORKUT\\com-orkut.ungraph.txt";
		
		/**
		 * Step-1: clean the community file, remove duplicates
		 * 	input: com-amazon.top5000.cmty.txt
		 *  output:com-amazon.top5000.cmty.clean.txt
		 */
		String refile="E:\\MyDropbox\\Dropbox\\Study\\SFU\\SFU-CourseStudy\\2014Fall-726-A3\\ASN\\project\\AMAZON\\subnetworks\\com-amazon.all.cmty.txt";
		removeDup(refile);
		
		/**
		 * Step-2: extra subnetworks according percent
		 * 	input1: com-amazon.top5000.cmty.clean.txt
		 * 	input2: com-amazon.ungraph.txt
		 * 	output1.1: com-amazon.top5000.cmty.clean0.05.txt
		 * 	output1.2: com-amazon.top5000.cmty.clean0.05.reindex.txt
		 * 	output2.1: com-amazon.ungraph0.05.small.txt
		 * 	output2.2: com-amazon.ungraph0.05.small.reindex.txt
		 */
		HashMap<Integer,Integer> nodes_index=DataPreprocess.extraCommunity(dfile,0.006);
		DataPreprocess.extraEdges(efile,nodes_index,0.006);
		

		/**
		 * extra as the paper
		 */
//		String datafile="E:\\MyDropbox\\Dropbox\\Study\\SFU\\SFU-CourseStudy\\2014Fall-726-A3\\ASN\\project\\AMAZON\\subnetworks\\com-amazon.all.cmty.clean.txt";
//		int sub_num=500;
//		DataPreprocess.extraSubnetworks(datafile,sub_num);
		
//		DataPreprocess.checkData("E:\\MyDropbox\\Dropbox\\Study\\SFU\\SFU-CourseStudy\\2014Fall-726-A3\\ASN\\project\\DBLP\\result\\com-dblp.ungraph.small.reindex_simimatrix_0.85_1.0000000000000004E-8.dat");

	}
	
	/**
	 * For Big Dataset
	 */
	//extra data from labeled big data sets
	//extra part communities
	public static HashMap<Integer,Integer> extraCommunity(String datafile,double percent) throws IOException
	{
		HashMap<Integer,Integer> nodes_index=new HashMap<Integer,Integer>();
		String line="";
		String output=datafile.substring(0,datafile.length()-4)+percent+".txt";
		String reoutput=datafile.substring(0,datafile.length()-4)+percent+".reindex.txt";
		FileWriter fw=new FileWriter(output);
		FileWriter refw=new FileWriter(reoutput);
		
		BufferedReader br=new BufferedReader(new FileReader(datafile));
		int total=0;
		while((line=br.readLine())!=null)
			total++;
		br.close();
		Random random=new Random();
		//extra percent data communities
		int extra=(int)(total*percent);
		System.out.println("start to extra data....");
		ArrayList<Integer> line_nums=new ArrayList<Integer>();
		int[] nums=new int[extra];
		int rd=0;
		for(int i=0;i<extra;i++)
		{
			//System.out.println("start for loop....");
			do
			{
				rd=random.nextInt(total);
			}while(line_nums.contains(rd));
			
			line_nums.add(rd);
			nums[i]=rd;
			//System.out.print(""+line_nums.get(i)+" ");
		}
		System.out.println("sorted result:");
		Arrays.sort(nums);
		for(int i=0;i<nums.length;i++)
			System.out.println(""+nums[i]+" ");
		
		System.out.println("end");
		br=new BufferedReader(new FileReader(datafile));
		int cnt=0;
		int j=0;
		int index=0;
		while((line=br.readLine())!=null&&j<extra)
		{
			if(cnt==nums[j])
			{
				fw.write(line+"\n");
				//
				StringTokenizer st=new StringTokenizer(line);
				String reline="";
				while(st.hasMoreTokens())
				{
					int ele=Integer.parseInt(st.nextToken());
					if(!nodes_index.containsKey(ele))
					{
						nodes_index.put(ele, index);
						reline+=Integer.toString(index)+"	";
						index++;
					}
					else
					{
						reline+=nodes_index.get(ele).toString()+"	";
					}
				}
				refw.write(reline.trim()+"\n");
				j++;
			}
			cnt++;
		}
		System.out.println("extra #line="+extra);
		System.out.println("extra #nodes="+nodes_index.size()+"--"+index);
		fw.close();
		refw.close();
		br.close();
		return nodes_index;
	}
	// extra the edges according the nodes from extra community's
	public static String extraEdges(String edgefile, HashMap<Integer,Integer> nodes_index,double percent) throws IOException
	{
		String output=edgefile.substring(0,edgefile.length()-4)+percent+".small.txt";
		String reoutput=edgefile.substring(0,edgefile.length()-4)+percent+".small.reindex.txt";
		FileWriter fw=new FileWriter(output);
		FileWriter refw=new FileWriter(reoutput);
		fw.write("Nodes: "+nodes_index.size()+"\n");
		refw.write("Nodes: "+nodes_index.size()+"\n");
		BufferedReader br=new BufferedReader(new FileReader(edgefile));
		String line="";
		int cnt=0;
		while((line=br.readLine())!=null)
		{
			if(line.contains("#"))
				continue;
			String[] parts=line.split("	");
			if(parts.length!=2)
				System.err.println("line format err!!");
			int from=Integer.parseInt(parts[0]);
			int to=Integer.parseInt(parts[1]);
			if(nodes_index.containsKey(from)&&nodes_index.containsKey(to))
			{
				fw.write(line+"\n");
				refw.write(""+nodes_index.get(from)+"	"+nodes_index.get(to)+"\n");
				refw.write(""+nodes_index.get(to)+"	"+nodes_index.get(from)+"\n");
				cnt++;
			}
		}
		br.close();
		fw.close();
		refw.close();
		System.out.println("extra #edges="+cnt);
		
		return output;
	}

	//remove duplicate records(repeat line)
	public static void removeDup(String datafile) throws IOException
	{
		String line="";
		String output=datafile.substring(0,datafile.length()-4)+".clean.txt";
		FileWriter fw=new FileWriter(output);
		
		BufferedReader br=new BufferedReader(new FileReader(datafile));
		int total=0;
		int cnt=0;
		ArrayList<String> context=new ArrayList<String>();
		while((line=br.readLine())!=null)
		{
			if(!context.contains(line))
			{
				context.add(line);
				fw.write(line+"\n");
				total++;
			}
			cnt++;
		}
		fw.close();
		br.close();
		System.out.println("before #line="+cnt);
		System.out.println("after #line="+context.size());
	}

	public static void checkData(String datafile) throws IOException
	{
		BufferedReader br=new BufferedReader(new FileReader(datafile));
		String output=datafile.substring(0,datafile.length()-4)+".clean.dat";
		FileWriter fw=new FileWriter(output);
		String line="";
		while((line=br.readLine())!=null)
		{
			String[] parts=line.trim().split("    ");
			
			String newline="";
			for(int i=0;i<parts.length;i++)
			{
				double e=Double.parseDouble(parts[i]);
				newline+=""+e+"    ";
				
			}
			newline=newline.trim();
			fw.write(newline+"\n");
		}
		fw.close();
		br.close();
	}

	//BIGCLAM evaluation:selected 500 subnetworks
	//TODO:unfinished...
	public static void extraSubnetworks(String datafile,int sub_num)throws IOException
	{
		String line="";
		
		BufferedReader br=new BufferedReader(new FileReader(datafile));
		ArrayList<String> communities=new ArrayList<String>(); //all communities
		HashMap<Integer,ArrayList<Integer>> node_comms=new HashMap<Integer,ArrayList<Integer>>();
		int cnt=0;
		while((line=br.readLine())!=null)
		{
			communities.add(line.trim());
			String[] parts=line.trim().split("	");
			for(int i=0;i<parts.length;i++)
			{
				int node=Integer.parseInt(parts[i].trim());
				if(node_comms.containsKey(node))
					node_comms.get(node).add(cnt);
				else
				{
					ArrayList<Integer> comms=new ArrayList<Integer>();
					comms.add(cnt);
					node_comms.put(node, comms);
				}
			}
			cnt++;
		}
		System.out.println("#of node_comms="+node_comms.size());
		//filter nodes that have less than two communities
		ArrayList<Integer> nodes=new ArrayList<Integer>(); //nodes have more than two communities
		Set<Map.Entry<Integer, ArrayList<Integer>>> sets = node_comms.entrySet();  
        for(Map.Entry<Integer, ArrayList<Integer>> entry : sets) { 
        	int key=entry.getKey();
        	ArrayList<Integer> value=entry.getValue();
        	if(value.size()>=2)
				nodes.add(key);
        } 
		
		System.out.println("#of nodes(>2comms)="+nodes.size());
		//random select sub_num=500 nodes that have at least two communities
		Random random=new Random();	
		int rd=0;
		ArrayList<Integer> selected_nodes=new ArrayList<Integer>();
		int total=nodes.size();
		for(int i=0;i<sub_num;i++)
		{
			do{
				rd=random.nextInt(total);
			}while(selected_nodes.contains(nodes.get(rd)));
			
			selected_nodes.add(nodes.get(rd));
		}
		//write the selected 500 communities
		//create a dir
		String dir=datafile.substring(0,datafile.lastIndexOf("\\"))+"\\communities";
		File file=new File(dir);
		file.mkdir();
		FileWriter[] fws=new FileWriter[sub_num];
		for(int i=0;i<sub_num;i++)
		{
			String output=datafile.substring(0,datafile.lastIndexOf("\\"))+"\\communities\\"+"subnetwork"+i+".txt";
			fws[i]=new FileWriter(output);
		}
		
		for(int i=0;i<selected_nodes.size();i++)
		{
			ArrayList<Integer> comms=node_comms.get(selected_nodes.get(i));
			for(int j=0;j<comms.size();j++)
			{
				fws[i].write(communities.get(comms.get(j))+"\n");
			}
			
		}
		
		for(int i=0;i<sub_num;i++)
		{
			fws[i].close();
		}
	}

	public static void extraSubedges(String dir,String edgefile)throws IOException
	{
		String line="";
		//read the edge file
		ArrayList<ArrayList<Integer>> edges=new ArrayList<ArrayList<Integer>>();
		BufferedReader edgebr=new BufferedReader(new FileReader(edgefile));
		while((line=edgebr.readLine())!=null)
		{
			String[] parts=line.split("	");
			ArrayList<Integer> pairs=new ArrayList<Integer>();
			for(int i=0;i<parts.length;i++)
			{
				int e=Integer.parseInt(parts[i]);
				pairs.add(e);
			}
		}
		edgebr.close();
		
		File commdir=new File(dir);
		File[] comms=commdir.listFiles();
		//new communities dir
		//old edge files dir
		//new edge files dir
		
		for(int i=0;i<comms.length;i++)
		{
			BufferedReader br=new BufferedReader(new FileReader(comms[i]));
			while((line=br.readLine())!=null)
			{
				
			}
		}
	}

	/**
	 * For small Dataset
	 * 	output format:
	 * 	Nodes: 1449
		248	260
		260	248
		248	269
		269	248
	 */
	
	
	
}
