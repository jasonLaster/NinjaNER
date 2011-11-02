import edu.stanford.nlp.ie.crf.*;
import edu.stanford.nlp.ie.AbstractSequenceClassifier;
import edu.stanford.nlp.io.IOUtils;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.ling.CoreAnnotations.AnswerAnnotation;
import edu.stanford.nlp.ling.Document;

import java.util.List;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringReader;
import java.net.URL;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;

import javax.servlet.ServletException;
import javax.servlet.http.*;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.*;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.json.JSONObject;
import org.json.XML;

public class ninjaWrapper extends HttpServlet{
        
    String serializedClassifier = "classifiers/all.3class.distsim.crf.ser.gz";
    AbstractSequenceClassifier classifier = CRFClassifier.getClassifierNoExceptions(serializedClassifier);

    
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {        
        String url = req.getParameter("url");
        
        if(url == null)
        {
            String html = readFile("ninja.html");
            resp.getWriter().print(html);
            return;
        }
        String contentAPI = "https://readability.com/api/content/v1/parser?url=" + url + "&token=a00a157998da82ec6561168c7a23670e770916fd";
        
        PrintWriter out = resp.getWriter();
        URL u = new URL(contentAPI);
        
        BufferedReader in = new BufferedReader(new InputStreamReader(u.openStream()));

        String content = "";
        String inputLine;
        String txt;
        while ((inputLine = in.readLine()) != null)
        {
            //resp.getWriter().print(inputLine);
            content+=inputLine;
//            txt = classifier.classifyWithInlineXML(inputLine);
//            while(txt.contains("<PERSON>"))
//            {
//                int start = txt.indexOf("<PERSON>");
//                int end = txt.indexOf("</PERSON>");
//                if(end < 0) break;
//                try{
//                    resp.getWriter().print(txt.substring(start + 8, end) + "\n");
//                    txt = txt.substring(end + 9);
//                } catch(Exception e)
//                {
//                    System.out.print("exception!\n");
//                    break;
//                }                
//            }
        }
        
        try{
            JSONObject j = new JSONObject(content);
            String c = j.get("content").toString();
            txt = classifier.classifyWithInlineXML(c);
            while(txt.contains("<PERSON>"))
              {
                  int start = txt.indexOf("<PERSON>");
                  int end = txt.indexOf("</PERSON>");
                  if(end < 0) break;
                  try{
                      resp.getWriter().print(txt.substring(start + 8, end) + "\n");
                      txt = txt.substring(end + 9);
                  } catch(Exception e)
                  {
                      System.out.print("exception!\n");
                      break;
                  }                
              }

        }
        catch (Exception e){
            e.printStackTrace();
        }
        in.close();
    }

    private static String readFile(String path) throws IOException {
        FileInputStream stream = new FileInputStream(new File(path));
        try {
          FileChannel fc = stream.getChannel();
          MappedByteBuffer bb = fc.map(FileChannel.MapMode.READ_ONLY, 0, fc.size());
          /* Instead of using default, pass in a decoder. */
          return Charset.defaultCharset().decode(bb).toString();
        }
        finally {
          stream.close();
        }
      }

    
    public static void main(String[] args) throws IOException {
        Server server = new Server(Integer.valueOf(System.getenv("PORT")));
        ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
        context.setContextPath("/");
        server.setHandler(context);
        context.addServlet(new ServletHolder(new ninjaWrapper()),"/*");
        try{
            server.start();
            server.join();
        } catch(Exception e)
        {
            e.printStackTrace();
            System.out.print(e.getMessage());
        }
    }
}
