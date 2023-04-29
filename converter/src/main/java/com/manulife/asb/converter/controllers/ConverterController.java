package com.manulife.asb.converter.controllers;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.util.ResourceUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import com.manulife.asb.converter.models.ASBEntity;

@Controller
public class ConverterController {

    Logger logger = LoggerFactory.getLogger(ConverterController.class);

    private ArrayList<Integer> randList = new ArrayList<>();

    @Value("${path.provider}")
    private String providerPath;
    @Value("${path.namespace}")
    private String namespacePath;    
    @Value("${path.queue}")
    private String queuePath;
    @Value("${path.topic}")
    private String topicPath;    
    @Value("${path.subscription}")
    private String subscriptionPath;
    @Value("${path.filter}")
    private String filterPath;
    
    private StringBuilder sb = new StringBuilder();
    private StringBuilder sbErr = new StringBuilder();
    private List<String> namespaces = Arrays.asList(
        "mfcsbuscust%env%%region%01",
        "mfcsbuspol%env%%region%01",
        "mfcsbusshare%env%%region%01",
        "mfcsbusclaim%env%%region%01",
        "mfcsbusagent%env%%region%01"
    );

    private boolean isFollowCsvNamespace = false;

    @GetMapping({"","/","/index"})
    public String index(){
        return "index";
    }
    
    @PostMapping("/upload")
    public String convert(@RequestParam("exportCsv") MultipartFile exportCsv, @RequestParam("env") String env, ModelMap m){
        logger.info("Environment :: {}", env);
        List<ASBEntity> asbEntities = convertCsv(exportCsv);

        String _env = env.toLowerCase().trim();
        int numberOfEnv = (_env.equals("prod") || _env.equals("dr")) ? 1 : 4;
        String RG = (_env.equals("uat") || _env.equals("prod")) ? "EAS-HKG-ASBM-PROD-01" : "EAS-HKG-ASBM-NONPROD-01";

        switch(_env){
            case "prd":
                RG = "EAS-HK-shared-Prod-01";
                isFollowCsvNamespace = true;
                break;
            case "uat":
                RG = "EAS-HK-agent-PreProd-01";
                isFollowCsvNamespace = true;
                break;
            case "prd-dr":
                RG = "SEA-SG-Custom-Prod-01";
                isFollowCsvNamespace = true;
                break;
            case "uat-dr":
                RG = "SEA-SG-shared-PreProd-01";
                isFollowCsvNamespace = true;
                break;
            case "dev":
            case "sit":
                RG = "EAS-HKG-ASBM-NONPROD-01";
                isFollowCsvNamespace = false;
                break;
            default:
                return "incorrect ENV";                            
        }

        String namespaceSuffix = env + (env.equals("dr") ? "sea" : "eas") + "01";

        prepareProvider();
        if(isFollowCsvNamespace){
            prepareUatPrdNamespace(env);
        } else {
            prepareNamespace(namespaces, RG, env);
        }

        prepareQueue(numberOfEnv, asbEntities.stream().filter(a -> a.getEntityType().equals("queue") && a.getNamespace().contains(namespaceSuffix)).toList());
        prepareTopic(numberOfEnv, asbEntities.stream().filter(a -> a.getEntityType().equals("topic") && a.getNamespace().contains(namespaceSuffix)).toList());
        prepareSubscription(numberOfEnv, asbEntities.stream().filter(a -> a.getEntityType().equals("subscription") && a.getNamespace().contains(namespaceSuffix)).toList());

        m.addAttribute("errMsg", sbErr.toString());        
        m.addAttribute("tfGenerated", sb.toString());
        return "output";
    }

    @SuppressWarnings("deprecation")
    private List<ASBEntity> convertCsv(MultipartFile exportCsv){
        List<ASBEntity> asbEntities = new ArrayList<>();
        try (
            //BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(csvPath), StandardCharsets.UTF_16))
            BufferedReader br = new BufferedReader(new InputStreamReader(exportCsv.getInputStream(), StandardCharsets.UTF_16))
        ){
            CSVParser parser = new CSVParser(br, 
                CSVFormat.DEFAULT.withDelimiter('\t').withFirstRecordAsHeader().withIgnoreHeaderCase().withTrim()
            );
            Iterable<CSVRecord> csvRecords = parser.getRecords();
            parser.close();
            
            for (CSVRecord csvRecord : csvRecords) {
                ASBEntity asb = new ASBEntity(
                    csvRecord.get(0),
                    csvRecord.get("ObjectName"),
                    csvRecord.get("Entity Name"),
                    csvRecord.get("Namespace"),
                    csvRecord.get("Topic Name (For subscription only)"),
                    csvRecord.get("MaxDelivery"),
                    csvRecord.get("TTL"),
                    csvRecord.get("LockDuration"),
                    csvRecord.get("ForwardTo"),
                    csvRecord.get("De-Dip"),
                    csvRecord.get("Session"),
                    csvRecord.get("TtlExpired"),
                    csvRecord.get("filter (For subscription only)")
                );

                asbEntities.add(asb);
            }
        } catch (IOException e) {
            e.printStackTrace();
            sbErr.append(e.getMessage());
        }
        return asbEntities;
    }

    private void prepareProvider(){
        sb.append(readTFFileContent(providerPath));
    }

    private void prepareUatPrdNamespace(String _env){
        String uatPrdnNamespacePath = "";
        switch(_env){
            case "prd":
                uatPrdnNamespacePath = "classpath:tf\\namespace-prd.txt";
                break;
            case "uat":
                uatPrdnNamespacePath = "classpath:tf\\namespace-uat.txt";
                break;
            case "prd-dr":
                uatPrdnNamespacePath = "classpath:tf\\namespace-prd-dr.txt";
                break;
            case "uat-dr":
                uatPrdnNamespacePath = "classpath:tf\\namespace-uat-dr.txt";
                break;
            default:
                uatPrdnNamespacePath = "classpath:tf\\namespace.txt";
                break;                       
        }        
        String namespaceTemplate = readTFFileContent(uatPrdnNamespacePath);
        sb.append(namespaceTemplate);
    }

    private void prepareNamespace(List<String> namespaces, String resourceGroup, String env){
        String namespaceTemplate = readTFFileContent(namespacePath);
        String namespaceContent = "";
        for(String namespace : namespaces){
            namespace = namespace.replace("%env%", env).replace("%region%", env.equals("dr") ? "sea" : "eas");
            namespaceContent = namespaceTemplate;
            namespaceContent = namespaceContent.replaceAll("%NSNAME%", namespace);
            namespaceContent = namespaceContent.replaceAll("%RGNAME%", resourceGroup);
            sb.append(namespaceContent);
        }
    }

    private void prepareQueue(int numberOfEnv, List<ASBEntity> asbEntities){
        try{
            String queueTemplate = readTFFileContent(queuePath);
            String queueContent = "";
            String special = "";

            for(ASBEntity asb : asbEntities){
                special = "";//reset
                if(!asb.getTtl().trim().equals("")){
                    special += "default_message_ttl                   = \"" + asb.getTtl() + "\"\r\n";
                }
                if(!asb.getLock().trim().equals("")){
                    special += "lock_duration                   = \"" + asb.getLock() + "\"\r\n";
                }
                if(!asb.getMaxDelivery().trim().equals("")){
                    special += "max_delivery_count                   = " + asb.getMaxDelivery() + "\r\n"; 
                }
                if(!asb.getForwardTo().trim().equals("")){
                    special += "forward_to                   = \"" + asb.getForwardTo() + "\"\r\n";
                }
                if(!asb.getDedip().trim().equals("")){
                    special += "requires_duplicate_detection                   = true" + "\r\n"; 
                    special += "duplicate_detection_history_time_window     = \"" + asb.getDedip() + "\"\r\n"; 
                }
                if(!asb.getSession().trim().equals("")){
                    special += "requires_session                   = true" + "\r\n"; 
                } 
                if(!asb.getTtlExpired().trim().equals("")){
                    special += "dead_lettering_on_message_expiration                   = true" + "\r\n"; 
                }                 

                for(int i = 1; i <= numberOfEnv; i++){
                    queueContent = queueTemplate;
                    queueContent = queueContent.replaceAll("%QUEUEOBJECTNAME%", asb.getObjectName() + "-0" + i);
                    queueContent = queueContent.replaceAll("%QUEUENAME%", asb.getEntityName() + "-0" + i);
                    queueContent = queueContent.replaceAll("%NSNAME%", asb.getNamespace());
                    queueContent = queueContent.replaceAll("%SPECIAL%", special);
                    sb.append(queueContent);
                }//end of multiple environment
            }//end of looping List<ASBEntity> asbEntities
        }catch(Exception e){
            e.printStackTrace();
            sbErr.append(e.getMessage());
        }
    }

    private void prepareTopic(int numberOfEnv, List<ASBEntity> asbEntities){
        try{
        String topicTemplate = readTFFileContent(topicPath);
        String topicContent = "";
        String special = "";

        for(ASBEntity asb : asbEntities){
            special = "";//reset
            if(!asb.getTtl().trim().equals("")){
                special += "default_message_ttl                   = \"" + asb.getTtl() + "\"\r\n"; 
            }
            if(!asb.getDedip().trim().equals("")){
                special += "requires_duplicate_detection                   = true\r\n";
                special += "duplicate_detection_history_time_window        = \"" + asb.getDedip() + "\"\r\n"; 
            }

            for(int i = 1; i <= numberOfEnv; i++){
                topicContent = topicTemplate;
                topicContent = topicContent.replaceAll("%TOPICNAME%", asb.getEntityName() + "-0" + i); //can't change this line or else will error in creating subscription
                topicContent = topicContent.replaceAll("%NSNAME%", asb.getNamespace());
                topicContent = topicContent.replaceAll("%SPECIAL%", special);
                sb.append(topicContent);
            }//end of multiple environment
        }//end of looping List<ASBEntity> asbEntities        
        }catch(Exception e){
            e.printStackTrace();
            sbErr.append(e.getMessage());
        }
    }

    private void prepareSubscription(int numberOfEnv, List<ASBEntity> asbEntities){
        try{
            String subscriptionTemplate = readTFFileContent(subscriptionPath);
            String filterTemplate = readTFFileContent(filterPath);
            String subscriptionContent = "", filterContent = "", filterValue = "";
            String special = "";
    
            for(ASBEntity asb : asbEntities){
                special = "";//reset
                if(!asb.getTtl().trim().equals("")){
                    special += "default_message_ttl                   = \"" + asb.getTtl() + "\"\r\n";
                }
                if(!asb.getLock().trim().equals("")){
                    special += "lock_duration                   = \"" + asb.getLock() + "\"\r\n";
                }
                if(!asb.getForwardTo().trim().equals("")){
                    special += "forward_to                   = \"" + asb.getForwardTo() + "\"\r\n";
                }
                if(!asb.getSession().trim().equals("")){
                    special += "requires_session                   = true\r\n";
                } 
                if(!asb.getTtlExpired().trim().equals("")){
                    special += "dead_lettering_on_message_expiration                   = true\r\n";
                }
                if(!asb.getFilter().trim().equals("")){
                    filterValue = asb.getFilter();
                }
    
                String subObjName = "";//randList
                int rand = 0;
                for(int i = 1; i <= numberOfEnv; i++){
                    subscriptionContent = subscriptionTemplate;
                    do{
                        rand = new Random(System.currentTimeMillis()).nextInt(1, 999);
                    }while(randList.contains(rand));
                    randList.add(rand);

                    String randHash = "-" + Integer.toString(rand);
                    subObjName = asb.getObjectName() + randHash + "-0" + i;
                    subscriptionContent = subscriptionContent.replaceAll("%SUBOBJECTNAME%", subObjName);
                    subscriptionContent = subscriptionContent.replaceAll("%SUBNAME%", asb.getEntityName() + "-0" + i);
                    subscriptionContent = subscriptionContent.replaceAll("%TOPICNAME%", asb.getTopicName() + "-0" + i);
                    subscriptionContent = subscriptionContent.replaceAll("%SPECIAL%", special);
                    subscriptionContent = subscriptionContent.replaceAll("%SUBMAXDEL%", !asb.getMaxDelivery().trim().equals("") ? asb.getMaxDelivery() : "10");
                    sb.append(subscriptionContent);

                    if(!filterValue.trim().equals("")){
                        filterContent = filterTemplate;
                        filterContent = filterContent.replaceAll("%SUBNAME%", subObjName);
                        filterContent = filterContent.replaceAll("%FILTERNAME%", "_" + Integer.toString(rand)  + "-rule-0" + i);
                        filterContent = filterContent.replaceAll("%FILTER%", filterValue);
                        sb.append(filterContent);
                    }
                }//end of multiple environment
            }//end of looping List<ASBEntity> asbEntities              
        }catch(Exception e){
            e.printStackTrace();
            sbErr.append(e.getMessage());
        }
    }

    private String readTFFileContent(String path){
        String content = "";
        try (
            BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(ResourceUtils.getFile(path))))
        ){
            StringBuilder sbContent = new StringBuilder();
            while((content = br.readLine()) != null){
                sbContent.append(content);
                sbContent.append("\r\n");
            }
            content = sbContent.toString();
        }catch(Exception e){
            e.printStackTrace();
            sbErr.append(e.getMessage());
        }

        return content;
    }
}
