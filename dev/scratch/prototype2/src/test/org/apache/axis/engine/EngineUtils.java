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

import java.io.IOException;
import java.net.ServerSocket;

import javax.xml.namespace.QName;

import org.apache.axis.description.AxisGlobal;
import org.apache.axis.description.AxisModule;
import org.apache.axis.description.AxisOperation;
import org.apache.axis.description.AxisService;
import org.apache.axis.description.EchoService;
import org.apache.axis.description.Flow;
import org.apache.axis.description.HandlerMetaData;
import org.apache.axis.description.MockFlow;
import org.apache.axis.description.Parameter;
import org.apache.axis.impl.description.ParameterImpl;
import org.apache.axis.impl.description.SimpleAxisOperationImpl;
import org.apache.axis.impl.description.SimpleAxisServiceImpl;
import org.apache.axis.impl.providers.SimpleJavaProvider;
import org.apache.axis.impl.transport.http.SimpleHTTPReceiver;

/**
 * @author Srinath Perera (hemapani@opensource.lk)
 */
public class EngineUtils {
    public static final int TESTING_PORT = 7777;
    public static final String FAILURE_MESSAGE = "Intentional Faliure";
    private static int index = 0; 
    
    public static void addHandlers(Flow flow,ExecutionChain exeChain,String phaseName) throws AxisFault{
        if(flow != null){
            int handlerCount = flow.getHandlerCount();
            for(int i = 0;i<handlerCount;i++){
                exeChain.addHandler(phaseName,flow.getHandler(i).getHandler());
            }
        }
    }
    
    public static SimpleHTTPReceiver startServer(EngineRegistry engineRegistry) throws IOException{
        AxisEngine engine = new AxisEngine(engineRegistry);
        ServerSocket serverSoc = new ServerSocket(TESTING_PORT);
        SimpleHTTPReceiver sas = new SimpleHTTPReceiver(engine);
        sas.setServerSocket(serverSoc);
        Thread thisThread = new Thread(sas);
        thisThread.setDaemon(true);
        thisThread.start();
        return sas;
    }
    
    public static void addHandler(Flow flow, Handler handler){
        HandlerMetaData hmd = new HandlerMetaData();
        hmd.setName(new QName("",String.valueOf(index)));
        index++;
        hmd.setHandler(handler);
        flow.addHandler(hmd);
    }
    
    public static void createExecutionChains(AxisService service) throws AxisFault{
        ExecutionChain inchain = new ExecutionChain();
        inchain.addPhase(new Phase(Constants.PHASE_SERVICE));
        EngineUtils.addHandlers(service.getInFlow(),inchain,Constants.PHASE_SERVICE);
        service.setExecutableInChain(inchain);

        ExecutionChain outchain = new ExecutionChain();
        outchain.addPhase(new Phase(Constants.PHASE_SERVICE));
        EngineUtils.addHandlers(service.getOutFlow(),outchain,Constants.PHASE_SERVICE);
        service.setExecutableOutChain(outchain);

        ExecutionChain faultchain = new ExecutionChain();

        faultchain.addPhase(new Phase(Constants.PHASE_SERVICE));

        EngineUtils.addHandlers(service.getFaultFlow(),faultchain,Constants.PHASE_SERVICE);
        service.setExecutableFaultChain(faultchain);
    }
    public static EngineRegistry createMockRegistry(QName serviceName,QName operationName,QName transportName) throws AxisFault{
        EngineRegistry engineRegistry = null;
        AxisGlobal global = new AxisGlobal();
        engineRegistry = new org.apache.axis.impl.engine.EngineRegistryImpl(global);
        

        
        AxisService service = new SimpleAxisServiceImpl(serviceName);
        service.setInFlow(new MockFlow("service inflow",4));
        service.setOutFlow(new MockFlow("service outflow",5));
        service.setFaultFlow(new MockFlow("service faultflow",1));
        service.setClassLoader(Thread.currentThread().getContextClassLoader());
        
        Parameter classParam = new ParameterImpl("className",EchoService.class.getName());
        service.addParameter(classParam);
         
        service.setProvider(new SimpleJavaProvider());
        
        AxisModule m1 = new AxisModule(new QName("","A Mdoule 1"));
        m1.setInFlow(new MockFlow("service module inflow",4));
        m1.setFaultFlow(new MockFlow("service module faultflow",1));
        service.addModule(m1.getName());
        
        AxisOperation operation = new SimpleAxisOperationImpl(operationName);
        
        service.addOperation(operation);
        engineRegistry.addService(service);
        //create Execution Chains
        ExecutionChain inchain = new ExecutionChain();
        inchain.addPhase(new Phase(Constants.PHASE_TRANSPORT));
        inchain.addPhase(new Phase(Constants.PHASE_GLOBAL));
        inchain.addPhase(new Phase(Constants.PHASE_SERVICE));
        EngineUtils.addHandlers(service.getInFlow(),inchain,Constants.PHASE_SERVICE);
        service.setExecutableInChain(inchain);
        
        ExecutionChain outchain = new ExecutionChain();
        outchain.addPhase(new Phase(Constants.PHASE_SERVICE));
        outchain.addPhase(new Phase(Constants.PHASE_GLOBAL));
        outchain.addPhase(new Phase(Constants.PHASE_TRANSPORT));
        EngineUtils.addHandlers(service.getOutFlow(),outchain,Constants.PHASE_SERVICE);
        service.setExecutableOutChain(outchain);
        
        ExecutionChain faultchain = new ExecutionChain();
        
        faultchain.addPhase(new Phase(Constants.PHASE_SERVICE));
        faultchain.addPhase(new Phase(Constants.PHASE_GLOBAL));
        faultchain.addPhase(new Phase(Constants.PHASE_TRANSPORT));
        
        EngineUtils.addHandlers(service.getFaultFlow(),faultchain,Constants.PHASE_SERVICE);
        service.setExecutableFaultChain(faultchain);
        return engineRegistry;
    }

}