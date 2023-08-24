import java.io.BufferedReader;
import java.io.FileReader;

public class extractor{

    private static String ph1 = "A resource with the ID ";
    private static String ph2 = " already exists";

    private static String ph3 = "   with ";
    private static String ph4 = ",";    

    private static String file = "C:\\Users\\chenkac\\OneDrive - Manulife\\Desktop\\error.txt";

    public static void main(String[] args){
        try{
            int l1 = ph1.length();
            int l3 = ph3.length();

            BufferedReader br = new BufferedReader(new FileReader(file));
            String subId = "";
            String resId = "";
            for(String line; (line = br.readLine()) != null; ) {

                if(line.contains(ph1)){
                    int pos1 = line.indexOf(ph1);
                    int pos2 = line.indexOf(ph2);
                    subId = line.substring(pos1 + l1 + 1, pos2 - 1);
                    resId = "";
                } else if (line.contains(ph3)){
                    int pos3 = line.indexOf(ph3);
                    int pos4 = line.indexOf(ph4);
                    resId = line.substring(pos3 + l3, pos4);
                    System.out.println("terraform import " + resId + " " + subId);
                    subId = "";
                }

            }
        }catch(Exception e){
            e.printStackTrace();
        }finally{

        }
    }

}