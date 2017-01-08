import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.*;
import java.util.Arrays;
import java.util.List;

public class APKUtil {

    public static void main(String[] args) throws Exception {
        List<String> argList = Arrays.asList(args);
        System.out.println("argList: " + argList);
        for (String s : args) {
            if (decompileApk(s) == 0) {
                if (modifyManifestFile(s)) {
                    buildApk(s);
                    signApk(s);
                    cleanDir(s);
                }
            }
        }
    }

    private static void cleanDir(String apk) throws Exception {
        String folder = apk.substring(apk.lastIndexOf("\\") + 1, apk.lastIndexOf(".apk"));
        String signedApk = apk.replace(".apk", "-signed.apk");

        String delDir = "cmd /C rd /S /Q \"" + new File(folder).getCanonicalPath() + "\"";
        String delFile = "cmd /C del /S /Q \"" + new File(folder + "-mod.apk").getCanonicalPath() + "\"";

        System.out.println("delDir: " + delDir);
        System.out.println("delFile: " + delFile);
        Process ps = Runtime.getRuntime().exec(delDir);
        new MyReader(ps.getErrorStream(), "error");
        new MyReader(ps.getInputStream(), "input");
        ps.waitFor();

        ps = Runtime.getRuntime().exec(delFile);
        new MyReader(ps.getErrorStream(), "error");
        new MyReader(ps.getInputStream(), "input");
        ps.waitFor();
    }

    private static int signApk(String apk) throws Exception {
        String folder = apk.substring(apk.lastIndexOf("\\") + 1, apk.lastIndexOf(".apk"));
        String signedApk = apk.replace(".apk", "-signed.apk");

        Process ps = Runtime.getRuntime().exec("java -jar signapk.jar certificate.pem key.pk8 \"" + folder + "-mod.apk\" " + "\"" + signedApk + "\"");
        new MyReader(ps.getErrorStream(), "error");
        new MyReader(ps.getInputStream(), "input");
        ps.waitFor();
        System.out.println("Signed apk: " + signedApk);
        return ps.exitValue();
    }

    private static boolean modifyManifestFile(String apk) throws Exception {
        String file = "AndroidManifest.xml";
        String folder = apk.substring(apk.lastIndexOf("\\") + 1, apk.lastIndexOf(".apk"));
        String fileLoc = folder + File.separator + file;
        System.out.print("fileLoc=" + fileLoc);
        File fXmlFile = new File(fileLoc);
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
        Document doc = dBuilder.parse(fXmlFile);

        doc.getDocumentElement().normalize();

        System.out.println("Root element: " + doc.getDocumentElement().getNodeName());

        Element e = doc.getDocumentElement();

        System.out.println("attribute: " + e.getAttribute("android:installLocation"));
        if ("preferExternal".equalsIgnoreCase(e.getAttribute("android:installLocation"))) {
            System.out.println("NO NEED TO MODIFY THE APK :)");
            return false;
        }
        e.setAttribute("android:installLocation", "preferExternal");
        System.out.println("attribute: " + e.getAttribute("android:installLocation"));

        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();
        DOMSource source = new DOMSource(doc);
        StreamResult result = new StreamResult(new File(fileLoc));

        // Output to console for testing
        // StreamResult result = new StreamResult(System.out);

        transformer.transform(source, result);

        System.out.println("Manifest file modified!");
        return true;
    }

    private static int decompileApk(String apk) throws Exception {
        Process ps = Runtime.getRuntime().exec("java -jar apktool_2.2.1.jar d \"" + apk + "\"");
        new MyReader(ps.getErrorStream(), "error");
        new MyReader(ps.getInputStream(), "input");
        ps.waitFor();
        System.out.println("Decompiled apk: " + apk);
        return ps.exitValue();
    }

    public static int buildApk(String apk) throws Exception {
        String folder = apk.substring(apk.lastIndexOf("\\") + 1, apk.lastIndexOf(".apk"));

        Process ps = Runtime.getRuntime().exec("java -jar apktool_2.2.1.jar b \"" + folder + "\" -o \"" + folder + "-mod.apk\"");
        new MyReader(ps.getErrorStream(), "error");
        new MyReader(ps.getInputStream(), "input");
        ps.waitFor();
        System.out.println("Built apk: " + folder + "-mod.apk");
        return ps.exitValue();
    }

    public static class MyReader implements Runnable {
        InputStream is;
        String streamName;

        MyReader(InputStream is, String streamName) {
            this.is = is;
            this.streamName = streamName;

            new Thread(this).start();
        }

        public void run() {
            BufferedReader br = new BufferedReader(new InputStreamReader(is));
            String line;
            //System.out.println(">>>> " + streamName.toUpperCase() + " STREAM >>>>");
            try {
                while ((line = br.readLine()) != null) {
                    System.out.println(line);
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {

                //System.out.println("<<<< " + streamName.toUpperCase() + " STREAM <<<<");
                    try {
                        br.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
            }
        }
    }
}
