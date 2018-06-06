/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package xacml;

import com.sun.xacml.ConfigurationStore;
import com.sun.xacml.EvaluationCtx;
import com.sun.xacml.Indenter;
import com.sun.xacml.PDP;
import com.sun.xacml.PDPConfig;
import com.sun.xacml.ParsingException;
import com.sun.xacml.UnknownIdentifierException;
import com.sun.xacml.cond.FunctionFactory;
import com.sun.xacml.cond.FunctionFactoryProxy;
import com.sun.xacml.cond.StandardFunctionFactory;
import com.sun.xacml.ctx.RequestCtx;
import com.sun.xacml.ctx.ResponseCtx;
import com.sun.xacml.finder.AttributeFinder;
import com.sun.xacml.finder.PolicyFinder;
import com.sun.xacml.finder.impl.CurrentEnvModule;
import com.sun.xacml.finder.impl.FilePolicyModule;
import com.sun.xacml.finder.impl.SelectorModule;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

/**
 *
 * @author jorgetorres
 */
public class Xacml {
    
    private PDP pdp = null;
    
    public Xacml(String configFilePath) throws Exception{
        ConfigurationStore store = new ConfigurationStore(new File(configFilePath));

        // use the default factories from the configuration
        store.useDefaultFactories();

        // get the PDP configuration's and setup the PDP
        pdp = new PDP(store.getDefaultPDPConfig());
    }

    public Xacml(String[] policyFiles){
        FilePolicyModule filePolicyModule = new FilePolicyModule();
        for (int i = 0; i < policyFiles.length; i++)
            filePolicyModule.addPolicy(policyFiles[i]);
        // next, setup the PolicyFinder that this PDP will use
        PolicyFinder policyFinder = new PolicyFinder();
        Set policyModules = new HashSet();
        policyModules.add(filePolicyModule);
        policyFinder.setModules(policyModules);

        // now setup attribute finder modules for the current date/time and
        // AttributeSelectors (selectors are optional, but this project does
        // support a basic implementation)
        CurrentEnvModule envAttributeModule = new CurrentEnvModule();
        SelectorModule selectorAttributeModule = new SelectorModule();
        // Setup the AttributeFinder just like we setup the PolicyFinder. Note
        // that unlike with the policy finder, the order matters here. See the
        // the javadocs for more details.
        AttributeFinder attributeFinder = new AttributeFinder();
        List attributeModules = new ArrayList();
        attributeModules.add(envAttributeModule);
        attributeModules.add(selectorAttributeModule);
        attributeFinder.setModules(attributeModules);

        /*
        // Try to load the time-in-range function, which is used by several
        // of the examples...see the documentation for this function to
        // understand why it's provided here instead of in the standard
        // code base.
        FunctionFactoryProxy proxy = StandardFunctionFactory.getNewFactoryProxy();
        FunctionFactory factory = proxy.getConditionFactory();
        factory.addFunction(new TimeInRangeFunction());
        FunctionFactory.setDefaultFactory(proxy);
        */

        // finally, initialize our pdp
        pdp = new PDP(new PDPConfig(attributeFinder, policyFinder, null));
    }
    
    public ResponseCtx evaluate(String requestFile) throws IOException, ParsingException, Exception
    {
        // setup the request based on the file
        RequestCtx request = RequestCtx.getInstance((Node)(getXMLDocument(requestFile).getDocumentElement()));
        // evaluate the request
        return pdp.evaluate(request);
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws Exception{
        if (args.length < 3) {
            System.out.println("Usage: -config <config> <request>");
            System.out.println("       <request> <policy> [policies]");
            System.exit(1);
        }
        
        Xacml simplePDP = null;
        String requestFile = null;
        
        if (args[0].equals("-config")) {
            simplePDP = new Xacml(args[1]);
            requestFile = args[2];
            ResponseCtx response = simplePDP.evaluate(requestFile);
            response.encode(System.out, new Indenter());
            
        } else {
            requestFile = args[0];
            
            for (int i = 1; i < args.length; i++) {
                simplePDP = new Xacml(new String[] { args[i] });
                ResponseCtx response = simplePDP.evaluate(requestFile);
                response.encode(System.out, new Indenter());
            }
        }
    }
    
    private static List<String> getFiles(String directoryLocation)
    {
        List<String> filesPaths = new ArrayList<String>();       
        File[] files = new File(directoryLocation).listFiles();
        for (File file : files) {
            if (file.isFile()) {
                filesPaths.add(file.getAbsolutePath());
            }
        }       
        return filesPaths;
    }
    
    private static Document getXMLDocument(String file) throws Exception {
        File xmlFile = new File(file);
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        dbFactory.setNamespaceAware(true);
        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
        Document doc = dBuilder.parse(xmlFile);
        return doc;
    }
}

