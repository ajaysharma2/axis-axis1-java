/*
 * Copyright 2004,2005 The Apache Software Foundation.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.axis.engine;



import java.io.File;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;

import junit.framework.TestCase;

import org.apache.axis.Constants;
import org.apache.axis.addressing.AddressingConstants;
import org.apache.axis.addressing.EndpointReference;
import org.apache.axis.clientapi.AsyncResult;
import org.apache.axis.clientapi.Callback;
import org.apache.axis.context.ConfigurationContext;
import org.apache.axis.context.EngineContextFactory;
import org.apache.axis.context.MessageContext;
import org.apache.axis.context.ServiceContext;
import org.apache.axis.deployment.DeploymentEngine;
import org.apache.axis.description.ModuleDescription;
import org.apache.axis.description.ServiceDescription;
import org.apache.axis.integration.UtilServer;
import org.apache.axis.om.OMAbstractFactory;
import org.apache.axis.om.OMElement;
import org.apache.axis.om.OMFactory;
import org.apache.axis.om.OMNamespace;
import org.apache.axis.phaseresolver.PhaseResolver;
import org.apache.axis.soap.SOAPEnvelope;
import org.apache.axis.soap.SOAPFactory;
import org.apache.axis.transport.http.SimpleHTTPServer;
import org.apache.axis.util.Utils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class EchoRawXMLOnTwoChannelsTest extends TestCase {
    private EndpointReference targetEPR =
        new EndpointReference(
            AddressingConstants.WSA_TO,
            "http://127.0.0.1:"
                + (UtilServer.TESTING_PORT)
                + "/axis/services/EchoXMLService/echoOMElement");
    private Log log = LogFactory.getLog(getClass());
    private QName serviceName = new QName("EchoXMLService");
    private QName operationName = new QName("echoOMElement");
    private QName transportName = new QName("http://localhost/my", "NullTransport");

    private AxisConfiguration engineRegistry;
    private MessageContext mc;
    private Thread thisThread;
    private SimpleHTTPServer sas;
    private ServiceContext serviceContext;
    
    private boolean finish = false;

    public EchoRawXMLOnTwoChannelsTest() {
        super(EchoRawXMLOnTwoChannelsTest.class.getName());
    }

    public EchoRawXMLOnTwoChannelsTest(String testName) {
        super(testName);
    }

    protected void setUp() throws Exception {
        UtilServer.start();
        UtilServer.getConfigurationContext().getEngineConfig().engageModule(new QName("addressing"));
        
        ServiceDescription service =
            Utils.createSimpleService(
                serviceName,
                org.apache.axis.engine.Echo.class.getName(),
                operationName);
        UtilServer.deployService(service);
        serviceContext =
            UtilServer.getConfigurationContext().createServiceContext(service.getName());

    }

    protected void tearDown() throws Exception {
        UtilServer.unDeployService(serviceName);
        UtilServer.stop();
    }

    private SOAPEnvelope createEnvelope(SOAPFactory fac) {
        SOAPEnvelope reqEnv = fac.getDefaultEnvelope();
        OMNamespace omNs = fac.createOMNamespace("http://localhost/my", "my");
        OMElement method = fac.createOMElement("echoOMElement", omNs);
        OMElement value = fac.createOMElement("myValue", omNs);
        value.addChild(fac.createText(value, "Isaac Assimov, the foundation Sega"));
        method.addChild(value);
        reqEnv.getBody().addChild(method);
        return reqEnv;
    }


    public void testEchoXMLCompleteASync() throws Exception {
        DeploymentEngine deploymentEngine = new DeploymentEngine();
        File file = new File("target/test-resources/repository-client/modules/addressing.mar");
        //File file = new File("modules/samples/target/test-resources/repository-client/modules/addressing.mar");
        
        assertTrue(file.exists());
        ModuleDescription moduleDesc =
            deploymentEngine.buildModule(file);

        EngineContextFactory efac = new EngineContextFactory();
        ConfigurationContext sysContext = efac.buildClientEngineContext(null);
        new ConfigurationContext(new AxisConfigurationImpl());
        sysContext.getEngineConfig().addMdoule(moduleDesc);
        sysContext.getEngineConfig().engageModule(moduleDesc.getName());
        ServiceDescription service =
                   Utils.createSimpleService(
                       serviceName,
                       org.apache.axis.engine.Echo.class.getName(),
                       operationName);
        sysContext.getEngineConfig().addService(service);
        Utils.resolvePhases(sysContext.getEngineConfig(),service);
        ServiceContext serviceContext = sysContext.createServiceContext(service.getName());
        
        
        

        OMFactory fac = OMAbstractFactory.getOMFactory();


        OMNamespace omNs = fac.createOMNamespace("http://localhost/my", "my");
        OMElement method = fac.createOMElement("echoOMElement", omNs);
        OMElement value = fac.createOMElement("myValue", omNs);
        value.setText("Isaac Assimov, the foundation Sega");
        method.addChild(value);


        org.apache.axis.clientapi.Call call = new org.apache.axis.clientapi.Call(serviceContext);

        call.setTo(targetEPR);
        call.setTransportInfo(Constants.TRANSPORT_HTTP, Constants.TRANSPORT_HTTP, true);
        Callback callback = new Callback() {
            public void onComplete(AsyncResult result) {
                try {
                    result.getResponseEnvelope().serialize(
                        XMLOutputFactory.newInstance().createXMLStreamWriter(System.out));
                } catch (XMLStreamException e) {
                    reportError(e);
                } finally {
                    finish = true;
                }
            }

            public void reportError(Exception e) {
                e.printStackTrace();
                finish = true;
            }
        };

        call.invokeNonBlocking(operationName.getLocalPart(), method, callback);
        int index = 0;
        while (!finish) {
            Thread.sleep(1000);
            index++;
            if(index > 10 ){
                throw new AxisFault("Server is shutdown as the Async response take too longs time");
            }
        }

        log.info("send the reqest");

}

}
