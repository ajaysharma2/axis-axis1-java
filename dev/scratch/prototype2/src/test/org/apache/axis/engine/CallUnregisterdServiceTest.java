/*
 * Copyright 2003,2004 The Apache Software Foundation.
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

//todo
import java.net.URL;

import javax.xml.namespace.QName;

import org.apache.axis.AbstractTestCase;
import org.apache.axis.client.Call;
import org.apache.axis.context.MessageContext;
import org.apache.axis.description.AxisOperation;
import org.apache.axis.description.AxisService;
import org.apache.axis.description.Parameter;
import org.apache.axis.impl.description.ParameterImpl;
import org.apache.axis.impl.description.SimpleAxisOperationImpl;
import org.apache.axis.impl.description.SimpleAxisServiceImpl;
import org.apache.axis.impl.providers.RawXMLProvider;
import org.apache.axis.impl.transport.http.SimpleHTTPReceiver;
import org.apache.axis.om.OMElement;
import org.apache.axis.om.OMFactory;
import org.apache.axis.om.OMNamespace;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * @author Srinath Perera(hemapani@opensource.lk)
 */
public class CallUnregisterdServiceTest extends AbstractTestCase{
    private Log log = LogFactory.getLog(getClass());
    private QName serviceName = new QName("","EchoXMLService");
    private QName operationName = new QName("http://localhost/my","echoOMElement");
    private QName transportName = new QName("http://localhost/my","NullTransport");

    private EngineRegistry engineRegistry;
    private MessageContext mc;
    private Thread thisThread = null;
    private SimpleHTTPReceiver sas;
    
    public CallUnregisterdServiceTest(){
        super(CallUnregisterdServiceTest.class.getName());
    }

    public CallUnregisterdServiceTest(String testName) {
        super(testName);
    }

    protected void setUp() throws Exception {
        engineRegistry = EngineUtils.createMockRegistry(serviceName,operationName,transportName);
        AxisService service = new SimpleAxisServiceImpl(serviceName);
        service.setClassLoader(Thread.currentThread().getContextClassLoader());
        Parameter classParam = new ParameterImpl("className",EchoXML.class.getName());
        service.addParameter(classParam);
        service.setProvider(new RawXMLProvider());
        AxisOperation operation = new SimpleAxisOperationImpl(operationName);
        
        service.addOperation(operation);
        EngineUtils.createExecutionChains(service);        
        engineRegistry.addService(service);
        
        sas = EngineUtils.startServer(engineRegistry);
    }

    protected void tearDown() throws Exception {
            sas.stop();   
            Thread.sleep(1000);
    }


    public void testEchoXMLSync() throws Exception{
        try{
            OMFactory fac = OMFactory.newInstance();

            OMNamespace omNs = fac.createOMNamespace("http://localhost/my","my");
            OMElement method =  fac.createOMElement("echoOMElement",omNs) ;
            OMElement value =  fac.createOMElement("myValue",omNs) ;
            value.setValue("Isaac Assimov, the foundation Sega");
            method.addChild(value);
            
            Call call = new Call();
            URL url = new URL("http","127.0.0.1",EngineUtils.TESTING_PORT,"/axis/services/EchoBadXMLService");
            OMElement omele = call.syncCall(method,url);
            assertNotNull(omele);
        }catch(AxisFault e){
            tearDown();
            return;
        }
        fail("the test must fail due ti bad service Name");    
    }
}