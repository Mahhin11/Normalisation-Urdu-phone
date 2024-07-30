package frontend;
import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import dto.Dataset;
import dto.Features;
import dto.InitialCluster;
import logic.LexC;
import utils.Constants;
import utils.Utils;

public class LexNorm {
	
	public static ConcurrentHashMap<String,Integer> frequentWordMap = new ConcurrentHashMap<String, Integer>();
	public static ArrayList<String> wordList = new ArrayList<String>();
	public static HashMap<String, Integer> wordMap = new HashMap<String,Integer>();
	public static HashMap<String,Integer> clusterID = new HashMap<String, Integer>();
	public static HashMap<String,Integer> stdClusterAssignment = new HashMap<String, Integer>();
	public static HashMap<String, ArrayList<Integer>> prevWordMap = new HashMap<String,ArrayList<Integer>>();
	public static HashMap<String, ArrayList<Integer>> nextWordMap = new HashMap<String,ArrayList<Integer>>();
	public static HashMap<String, ArrayList<Integer>> prevUrduPhoneMap = new HashMap<String,ArrayList<Integer>>();
	public static HashMap<String, ArrayList<Integer>> nextUrduPhoneMap = new HashMap<String,ArrayList<Integer>>();
	public static HashMap<String, String> UrduPhoneMap = new HashMap<String, String>();
	public static HashMap<String, String> UrduPhoneCodes = new HashMap<String, String>();
	public static HashMap<String,Integer> clusterAssignment = new HashMap<String, Integer>();
	public static HashMap<Integer,ArrayList<String>> clusters = new HashMap<Integer, ArrayList<String>>();
	public static HashMap<Integer,String> results = new HashMap<Integer,String>();

	//Update the 3 parameters below for different experimental scenario
	public static String dataset = Dataset.DEFAULT.getName();
	public static InitialCluster initial = InitialCluster.URDUPHONE;
	public static Features features = Features.URDUPHONE_EDITDISTANCE_WORDID;
	
	public static String dataPath = "Input Files\\"+dataset+"\\";
	public static String inputPath = "Input Files\\";
	public static String standardFile = "Gold Standard.txt";
	public static int contextSize = 5;
	
	public static void main(String[] args) {
		
		loadInput();
		System.out.println("Input Read!");
		
		if(features == Features.URDUPHONE)
			Constants.outputFile = dataset+" - Experiment=1 P="+Constants.w_UrduPhone+" C_Size="+contextSize+" t="+Constants.threshold+".txt";
		else if(features == Features.EDITDISTANCE)
			Constants.outputFile = dataset+" - Experiment=2 S="+Constants.w_ED+" C_Size="+contextSize+" t="+Constants.threshold+".txt";
		else if(features == Features.URDUPHONE_EDITDISTANCE)
			Constants.outputFile = dataset+" - Experiment=3 P="+Constants.w_UrduPhone+" S="+Constants.w_ED+" C_Size="+contextSize+" t="+Constants.threshold+".txt";
		else if(features == Features.URDUPHONE_EDITDISTANCE_URDUPHONEID)
			Constants.outputFile = dataset+" - Experiment=4 P="+Constants.w_UrduPhone+" S="+Constants.w_ED+" C="+Constants.w_nextUrduPhone+" C_Size="+contextSize+" t="+Constants.threshold+".txt";
		else if(features == Features.URDUPHONE_EDITDISTANCE_WORDID)
			Constants.outputFile = dataset+" - Experiment=5 P="+Constants.w_UrduPhone+" S="+Constants.w_ED+" C="+Constants.w_nextWord+" C_Size="+contextSize+" t="+Constants.threshold+".txt";
		else{
			System.out.println("Incorrect Feature Set!");
			System.exit(0);
		}
		
		LexC.cluster();// Cluster method call 
		
		System.out.println("Dataset = "+dataset);
		System.out.println("Word Count Original = "+wordMap.keySet().size());
		System.out.println("Word Count Final = "+results.keySet().size());
		System.out.println("DONE!");
	}

	private static void loadInput() {
		
		// Declare counters for insufficient context
		int removedDueToInsufficientPrevContext = 0;
		int removedDueToInsufficientNextContext = 0;
		int removedDueToInsufficientPrevUrduPhoneContext = 0;
		int removedDueToInsufficientNextUrduPhoneContext = 0;

		
		// Declare counters for each removal reason
		int removedDueToNextWordMap = 0;
		int removedDueToPrevUrduPhoneMap = 0;
		int removedDueToNextUrduPhoneMap = 0;
		int removedDueToStdClusterAssignment = 0;
		int removedDueToPrevWordMap = 0;
		int removedDueToNoClusterAssignment = 0;

		// Counters for how many lists each word was missing from
		int singleCountRemoved = 0;
		int doubleCountRemoved = 0;
		int tripleCountRemoved = 0;
		int quadCountRemoved = 0;
		int quintCountRemoved = 0;

	    BufferedReader br;
	    String file = "";
	    try {
	        String line = "";
	        String val[];
	        int id = 0;

	        file = dataset + " - Word List.txt";
	        br = new BufferedReader(new FileReader(dataPath + file));
	        System.out.println("Reading file '" + dataPath + file + "'");
	        id = 1;
	        while ((line = br.readLine()) != null) {
	            val = line.split(" ");
	            wordList.add(val[0]);
	            wordMap.put(val[0], id);
	            id++;
	        }
	        br.close();

	        int initialWordCount = wordList.size();
	        System.out.println("Initial Word Count: " + initialWordCount);  // Print initial word count

	        // Read other required files and populate maps
	        file = standardFile;
	        br = new BufferedReader(new FileReader(inputPath + file));
	        System.out.println("Reading file '" + inputPath + file + "'");
	        while ((line = br.readLine()) != null) {
	            id++;
	            val = line.split(",");
	            clusterID.put(val[1], id);
	        }
	        br.close();

	        file = dataset + " - Frequent Word List.txt";
	        br = new BufferedReader(new FileReader(dataPath + file));
	        System.out.println("Reading file '" + dataPath + file + "'");
	        while ((line = br.readLine()) != null) {
	            val = line.split(" ");
	            frequentWordMap.put(val[0], 1);
	        }
	        br.close();
	        file = standardFile;
	        br = new BufferedReader(new FileReader(inputPath + file));

	        while ((line = br.readLine()) != null) {
	            val = line.split(",");
	            stdClusterAssignment.put(val[0], clusterID.get(val[1]));
	            if (frequentWordMap.get(val[1]) != null)
	                stdClusterAssignment.put(val[1], clusterID.get(val[1]));
	        }
	        br.close();
	        ArrayList<Integer> list; // many words get removed here
	        file = dataset + " - " + contextSize + " Prev Word ID List.txt";
	        br = new BufferedReader(new FileReader(dataPath + file));
	        System.out.println("Reading file '" + dataPath + file + "'");
	        while ((line = br.readLine()) != null) {
	            list = new ArrayList<Integer>();
	            val = line.split(",");
	            if (val.length < contextSize + 1) {
	                System.out.println("Removing word (insufficient prev word context): " + val[0]);
	                removedDueToInsufficientPrevContext++;
	                continue; // lines with less than required context size will get skipped
	            }
	            for (int i = 1; i < val.length; i++)
	                list.add(Integer.parseInt(val[i])); // data type conversion
	            prevWordMap.put(val[0], list); // list contains the ids, and val the word put back into dict
	        }
	        br.close();

	        // Load Next Word Context
	        file = dataset + " - " + contextSize + " Next Word ID List.txt";
	        br = new BufferedReader(new FileReader(dataPath + file));
	        System.out.println("Reading file '" + dataPath + file + "'");
	        while ((line = br.readLine()) != null) {
	            list = new ArrayList<Integer>();
	            val = line.split(",");
	            if (val.length < contextSize + 1) {
	                System.out.println("Removing word (insufficient next word context): " + val[0]);
	                removedDueToInsufficientNextContext++;
	                continue; // lines with less than required context size will get skipped
	            }
	            for (int i = 1; i < val.length; i++)
	                list.add(Integer.parseInt(val[i]));
	            nextWordMap.put(val[0], list);
	        }
	        br.close();

	        // Load Previous UrduPhone Context
	        file = dataset + " - " + contextSize + " Prev UrduPhone ID List.txt";
	        br = new BufferedReader(new FileReader(dataPath + file));
	        System.out.println("Reading file '" + dataPath + file + "'");
	        while ((line = br.readLine()) != null) {
	            list = new ArrayList<Integer>();
	            val = line.split(",");
	            if (val.length < contextSize + 1) {
	                System.out.println("Removing word (insufficient prev UrduPhone context): " + val[0]);
	                removedDueToInsufficientPrevUrduPhoneContext++;
	                continue; // lines with less than required context size will get skipped
	            }
	            for (int i = 1; i < val.length; i++)
	                list.add(Integer.parseInt(val[i]));
	            prevUrduPhoneMap.put(val[0], list);
	        }
	        br.close();

	        // Load Next UrduPhone Context
	        file = dataset + " - " + contextSize + " Next UrduPhone ID List.txt";
	        br = new BufferedReader(new FileReader(dataPath + file));
	        System.out.println("Reading file '" + dataPath + file + "'");
	        while ((line = br.readLine()) != null) {
	            list = new ArrayList<Integer>();
	            val = line.split(",");
	            if (val.length < contextSize + 1) {
	                System.out.println("Removing word (insufficient next UrduPhone context): " + val[0]);
	                removedDueToInsufficientNextUrduPhoneContext++;
	                continue; // lines with less than required context size will get skipped
	            }
	            for (int i = 1; i < val.length; i++)
	                list.add(Integer.parseInt(val[i]));
	            nextUrduPhoneMap.put(val[0], list);
	        }
	        br.close();
	        

	        file = dataset + " - UrduPhone - ID.txt";
	        br = new BufferedReader(new FileReader(dataPath + file));
	        System.out.println("Reading file '" + dataPath + file + "'");
	        while ((line = br.readLine()) != null) {
	            val = line.split(",");
	            UrduPhoneMap.put(val[0], val[1]);
	            if (initial == InitialCluster.URDUPHONE) {
	                id = Integer.parseInt(val[2]);
	                clusterAssignment.put(val[0], id);
	            }
	            UrduPhoneCodes.put(val[1], "1");
	        }
	        br.close();

	        if (initial == InitialCluster.RANDOM_URDUPHONESIZE) {
	            Random r = new Random();
	            for (String word : wordList) {
	                id = r.nextInt(UrduPhoneCodes.size());
	                clusterAssignment.put(word, id);
	            }
	        }

	        if (initial == InitialCluster.RANDOM_SINGLE) {
	            id = 0;
	            for (String word : wordList) {
	                id++;
	                clusterAssignment.put(word, id);
	            }
	        }

	     // Perform word removal with detailed logging
	     // Perform word removal with detailed logging
	     // Perform word removal with detailed logging
	     // Perform word removal with detailed logging
	        for (String word : wordMap.keySet()) {
	            int missingCount = 0;

	            boolean missingNextWordMap = nextWordMap.get(word) == null;
	            boolean missingPrevUrduPhoneMap = prevUrduPhoneMap.get(word) == null;
	            boolean missingNextUrduPhoneMap = nextUrduPhoneMap.get(word) == null;
	            boolean missingStdClusterAssignment = stdClusterAssignment.get(word) == null;
	            boolean missingPrevWordMap = prevWordMap.get(word) == null;
	            boolean noClusterAssignment = clusterAssignment.get(word) == null;

	            // Check each condition and count how many are missing
	            if (missingNextWordMap) {
	                missingCount++;
	            }
	            if (missingPrevUrduPhoneMap) {
	                missingCount++;
	            }
	            if (missingNextUrduPhoneMap) {
	                missingCount++;
	            }
	            if (missingStdClusterAssignment) {
	                missingCount++;
	            }
	            if (missingPrevWordMap) {
	                missingCount++;
	            }
	            if (noClusterAssignment) {
	                missingCount++;
	            }

	            // Remove the word if it's missing from any required list and update the appropriate counters
	            if (missingCount > 0) {
	                System.out.println("Removing word (missing from " + missingCount + " lists): " + word);
	                wordList.remove(word);
	                clusterAssignment.remove(word);

	                if (missingNextWordMap) {
	                    removedDueToNextWordMap++;
	                }
	                if (missingPrevUrduPhoneMap) {
	                    removedDueToPrevUrduPhoneMap++;
	                }
	                if (missingNextUrduPhoneMap) {
	                    removedDueToNextUrduPhoneMap++;
	                }
	                if (missingStdClusterAssignment) {
	                    removedDueToStdClusterAssignment++;
	                }
	                if (missingPrevWordMap) {
	                    removedDueToPrevWordMap++;
	                }
	                if (noClusterAssignment) {
	                    removedDueToNoClusterAssignment++;
	                }

	                // Update counters for how many lists the word was missing from
	                if (missingCount == 1) {
	                    singleCountRemoved++;
	                } else if (missingCount == 2) {
	                    doubleCountRemoved++;
	                } else if (missingCount == 3) {
	                    tripleCountRemoved++;
	                } else if (missingCount == 4) {
	                    quadCountRemoved++;
	                } else if (missingCount == 5) {
	                    quintCountRemoved++;
	                }
	            }
	        }
	     // Print the counters for insufficient context
	        System.out.println("Number of words removed due to insufficient prev word context: " + removedDueToInsufficientPrevContext);
	        System.out.println("Number of words removed due to insufficient next word context: " + removedDueToInsufficientNextContext);
	        System.out.println("Number of words removed due to insufficient prev UrduPhone context: " + removedDueToInsufficientPrevUrduPhoneContext);
	        System.out.println("Number of words removed due to insufficient next UrduPhone context: " + removedDueToInsufficientNextUrduPhoneContext);
	        System.out.println("Those were the words with Lack of Context "+"\n"+"Next are the counters for Null Entries in Files");

	        // Print the counters
	        System.out.println("Number of words removed due to missing next word mapping: " + removedDueToNextWordMap);
	        System.out.println("Number of words removed due to missing previous UrduPhone mapping: " + removedDueToPrevUrduPhoneMap);
	        System.out.println("Number of words removed due to missing next UrduPhone mapping: " + removedDueToNextUrduPhoneMap);
	        System.out.println("Number of words removed due to missing standard cluster assignment: " + removedDueToStdClusterAssignment);
	        System.out.println("Number of words removed due to missing previous word mapping: " + removedDueToPrevWordMap);
	        System.out.println("Number of words removed due to no cluster assignment: " + removedDueToNoClusterAssignment);
	        System.out.println("Number of words removed missing in 1 list: " + singleCountRemoved);
	        System.out.println("Number of words removed missing in 2 lists: " + doubleCountRemoved);
	        System.out.println("Number of words removed missing in 3 lists: " + tripleCountRemoved);
	        System.out.println("Number of words removed missing in 4 lists: " + quadCountRemoved);
	        System.out.println("Number of words removed missing in 5 lists: " + quintCountRemoved);


	        int finalWordCount = wordList.size();
	        System.out.println("Initial Word Count: " + initialWordCount);  // Print initial word count

	        System.out.println("Final Word Count: " + finalWordCount);
	        System.out.println("Words Dropped: " + (initialWordCount - finalWordCount));

	        makeClusters();
	        Utils.setConstants();
	    } catch (Exception ex) {
	        ex.printStackTrace();
	    }
	}

	public static void makeClusters(){
		Set<String> words = clusterAssignment.keySet();
		int id = 0;
		ArrayList<String> cluster;
		clusters = new HashMap<Integer, ArrayList<String>>();
		for(String word:words){
			id = clusterAssignment.get(word);   // Word key and id values we get that   
			cluster = clusters.get(id);// for clusters heatmap , id key and cluster values , Check if the id already belonggs to cluster 
			if(cluster == null) // don't belong lets create , new cluster for this and add the value to that cluster 
				
				cluster = new ArrayList<String>();
			cluster.add(word);
			clusters.put(id, cluster);// Update our clusters dictionalry 
		}
	}
}