package org.apache.axis.phaserule;

import java.util.ArrayList;

import javax.xml.namespace.QName;

import org.apache.axis.AbstractTestCase;
import org.apache.axis.description.GlobalDescription;
import org.apache.axis.description.HandlerDescription;
import org.apache.axis.description.PhaseRule;
import org.apache.axis.engine.AxisConfiguration;
import org.apache.axis.engine.AxisSystemImpl;
import org.apache.axis.engine.Handler;
import org.apache.axis.phaseresolver.PhaseHolder;

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
*
*
*/

/**
 * Author : Deepal Jayasinghe
 * Date: May 10, 2005
 * Time: 4:28:27 PM
 */
public class PreDispatchPhaseRuleTest  extends AbstractTestCase{

    PreDispatchPhaseRuleTest phaserul;
    AxisConfiguration registry;

    public PreDispatchPhaseRuleTest(String testName) {
        super(testName);
    }

    public void testPhaseRule() throws Exception {
        phaserul = new PreDispatchPhaseRuleTest("");
        GlobalDescription global = new GlobalDescription();
        registry = new AxisSystemImpl(global);
        ArrayList inPhase = new ArrayList();

        inPhase.add("global");
        inPhase.add("transport");
        inPhase.add("Logging");
        inPhase.add("service");
        ((AxisSystemImpl) registry).setInPhases(inPhase);
        ((AxisSystemImpl) registry).setInFaultPhases(inPhase);
        ((AxisSystemImpl) registry).setOutFaultPhases(inPhase);
        ((AxisSystemImpl) registry).setOutPhases(inPhase);

        Handler han = null;
        PhaseHolder ph = new PhaseHolder(registry);
        ph.setFlowType(1);


        HandlerDescription pre = new HandlerDescription();
        pre.setClassName("org.apache.axis.handlers.AbstractHandler");
        pre.setHandler(han);
        pre.setName(new QName("pre-H1"));
        PhaseRule pre_rule1 = new PhaseRule();
        pre_rule1.setPhaseName("pre-dispatch");
        pre.setRules(pre_rule1);
        ph.addHandler(pre);

        HandlerDescription pre2 = new HandlerDescription();
        pre2.setClassName("org.apache.axis.handlers.AbstractHandler");
        pre2.setHandler(han);
        pre2.setName(new QName("pre-H2"));
        PhaseRule prerule2 = new PhaseRule();
        prerule2.setPhaseName("pre-dispatch");
        pre2.setRules(prerule2);
        ph.addHandler(pre2);


        HandlerDescription hm = new HandlerDescription();
        hm.setClassName("org.apache.axis.handlers.AbstractHandler");
        hm.setHandler(han);
        hm.setName(new QName("H1"));
        PhaseRule rule = new PhaseRule();
        rule.setPhaseName("global");
        rule.setPhaseFirst(true);
        hm.setRules(rule);
        ph.addHandler(hm);

        HandlerDescription hm1 = new HandlerDescription();
        hm1.setClassName("org.apache.axis.handlers.AbstractHandler");
        hm1.setHandler(han);
        hm1.setName(new QName("H2"));
        PhaseRule rule1 = new PhaseRule();
        rule1.setPhaseName("global");
        rule1.setAfter("H1");
        hm1.setRules(rule1);
        ph.addHandler(hm1);

        HandlerDescription hm2 = new HandlerDescription();
        hm2.setClassName("org.apache.axis.handlers.AbstractHandler");
        hm2.setHandler(han);
        hm2.setName(new QName("H3"));
        PhaseRule rule2 = new PhaseRule();
        rule2.setPhaseName("global");
        rule2.setAfter("H1");
        rule2.setBefore("H2");
        hm2.setRules(rule2);
        ph.addHandler(hm2);

        HandlerDescription hm3 = new HandlerDescription();
        hm3.setClassName("org.apache.axis.handlers.AbstractHandler");
        hm3.setHandler(han);
        hm3.setName(new QName("H4"));
        PhaseRule rule3 = new PhaseRule();
        rule3.setPhaseName("Logging");
        hm3.setRules(rule3);
        ph.addHandler(hm3);


        ArrayList oh = ph.getOrderHandler();
        for (int i = 0; i < oh.size(); i++) {
            HandlerDescription metadata = (HandlerDescription) oh.get(i);
            System.out.println("Name:" + metadata.getName().getLocalPart());
        }
    }
}