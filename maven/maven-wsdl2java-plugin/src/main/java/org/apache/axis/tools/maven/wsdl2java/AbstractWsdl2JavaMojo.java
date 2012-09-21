/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.axis.tools.maven.wsdl2java;

import java.io.File;
import java.net.MalformedURLException;

import javax.xml.namespace.QName;
import javax.xml.rpc.ServiceFactory;

import org.apache.axis.constants.Scope;
import org.apache.axis.tools.maven.shared.nsmap.Mapping;
import org.apache.axis.tools.maven.shared.nsmap.MappingUtil;
import org.apache.axis.wsdl.gen.GeneratorFactory;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;

import com.github.veithen.ulog.PlexusLoggerInjector;

public abstract class AbstractWsdl2JavaMojo extends AbstractMojo {
    /**
     * @component
     */
    // This is necessary to set up logging such that all messages logged by the Axis
    // libraries through commons logging are redirected to Plexus logs.
    PlexusLoggerInjector loggerInjector;
    
    /**
     * The maven project.
     *
     * @parameter expression="${project}"
     * @required
     * @readonly
     */
    private MavenProject project;
    
    /**
     * The WSDL file to process.
     * 
     * @parameter
     */
    private File file;
    
    /**
     * The URL of the WSDL to process. This should only be used for remote WSDLs. For local files,
     * use the <code>file</code> parameter.
     * 
     * @parameter
     */
    private String url;
    
    /**
     * Add scope to deploy.xml: "Application", "Request", "Session".
     * 
     * @parameter
     */
    private String deployScope;

    /**
     * Mappings of namespaces to packages.
     * 
     * @parameter
     */
    private Mapping[] mappings;
    
    /**
     * The default type mapping registry to use. Either 1.1 or 1.2.
     * 
     * @parameter default-value="1.2"
     * @required
     */
    private String typeMappingVersion;
    
    /**
     * <p>
     * Specifies what artifacts should be generated. Valid values are:
     * <ul>
     * <li><code>client</code>: generate client stubs
     * <li><code>server</code>: generate server side artifacts
     * <li><code>both</code>: generate all artifacts
     * </ul>
     * <p>
     * The <code>server</code> mode can also be used for clients that rely on dynamic proxies
     * created using the JAX-RPC {@link ServiceFactory} API, because they don't need client stubs.
     * <p>
     * Also note that the <code>both</code> mode is only really meaningful if {@link #skeleton} is
     * set to <code>true</code> or if {@link #deployWsdd} is specified. If none of these conditions
     * is satisfied, then <code>client</code> and <code>both</code> will generate the same set of
     * artifacts.
     * 
     * @parameter
     * @required
     */
    private String generate;
    
    /**
     * Set the name of the class implementing the web service.
     * This is especially useful when exporting a java class
     * as a web service using Java2WSDL followed by WSDL2Java.
     * This parameter is ignored if {@link #generate} is set to <code>client</code>.
     * 
     * @parameter
     */
    private String implementationClassName;
    
    /**
     * deploy skeleton (true) or implementation (false) in deploy.wsdd.
     * Default is false. This parameter is ignored if {@link #generate} is set to
     * <code>client</code>.
     * 
     * @parameter default-value="false"
     */
    private boolean skeleton;
    
    /**
     * flag to generate code for all elements, even unreferenced ones
     * 
     * @parameter default-value="false"
     */
    private boolean all;
    
    /**
     * Set the wrap arrays flag - if true this will make new classes
     * like "ArrayOfString" for literal "wrapped" arrays.  Otherwise it
     * will use "String []" and generate appropriate metadata.
     * 
     * @parameter default-value="false"
     */
    private boolean wrapArrays;
    
    /**
     * Set the noWrapped flag.
     * 
     * @parameter default-value="false"
     */
    private boolean noWrapped;
    
    /**
     * Turn on/off Helper class generation.
     * 
     * @parameter default-value="false"
     */
    private boolean helperGen;
    
    /**
     * The location of the deployment WSDD file to be generated. This parameter is ignored if
     * {@link #generate} is set to <code>client</code>. If this parameter is not specified, then no
     * deployment WSDD will be generated.
     * 
     * @parameter
     */
    private File deployWsdd;
    
    /**
     * The location of the undeployment WSDD file to be generated. This parameter is ignored if
     * {@link #generate} is set to <code>client</code>. If this parameter is not specified, then no
     * undeployment WSDD will be generated. Note that (in contrast to {@link #deployWsdd}) this
     * parameter is rarely used: in general, no undeployment WSDD is required.
     * 
     * @parameter
     */
    private File undeployWsdd;
    
    /**
     * A set of Java to XML type mappings that override the default mappings. This can be used to
     * change the Java class associated with an XML type.
     * 
     * @parameter
     */
    private JavaXmlTypeMapping[] javaXmlTypeMappings;
    
    public void execute() throws MojoExecutionException, MojoFailureException {
        String wsdlUrl;
        if (file != null && url != null) {
            throw new MojoFailureException("Invalid plugin configuration: either use file or url, but not both!");
        } else if (file != null) {
            try {
                wsdlUrl = file.toURL().toExternalForm();
            } catch (MalformedURLException ex) {
                throw new MojoExecutionException("Unexpected exception", ex);
            }
        } else if (url != null) {
            wsdlUrl = url;
        } else {
            throw new MojoFailureException("Invalid plugin configuration: file or url must be given!");
        }
        
        // Instantiate the emitter
        EmitterEx emitter = new EmitterEx();
        if (generate.equals("client")) {
            emitter.setClientSide(true);
            emitter.setServerSide(false);
        } else if (generate.equals("server")) {
            emitter.setClientSide(false);
            emitter.setServerSide(true);
        } else if (generate.equals("both")) {
            emitter.setClientSide(true);
            emitter.setServerSide(true);
        } else {
            throw new MojoExecutionException("Invalid value '" + generate + "' for the 'generate' parameter");
        }
        if (deployWsdd != null) {
            emitter.setDeployWsdd(deployWsdd.getAbsolutePath());
        }
        if (undeployWsdd != null) {
            emitter.setUndeployWsdd(undeployWsdd.getAbsolutePath());
        }
        emitter.setFactory(new JavaGeneratorFactoryEx(emitter));

        //extract the scope
        Scope scope = Scope.getScope(deployScope, null);
        if (scope != null) {
            emitter.setScope(scope);
        } else if (deployScope != null) {
            getLog().warn("Unrecognized scope:  " + deployScope + ".  Ignoring it.");
        }

        //do the mappings, with namespaces mapped as the key
        if (mappings != null && mappings.length > 0) {
            emitter.setNamespaceMap(MappingUtil.getNamespaceToPackageMap(mappings));
        }
//        emitter.setTestCaseWanted(testCase);
        emitter.setHelperWanted(helperGen);
//        emitter.setNamespaceIncludes(nsIncludes);
//        emitter.setNamespaceExcludes(nsExcludes);
//        emitter.setProperties(properties);
//        emitter.setImports(!noImports);
        emitter.setAllWanted(all);
        emitter.setOutputDir(getSourceOutputDirectory().getAbsolutePath());
        emitter.setSkeletonWanted(skeleton);
//        emitter.setVerbose(verbose);
//        emitter.setDebug(debug);
//        emitter.setQuiet(quiet);
        emitter.setTypeMappingVersion(typeMappingVersion);
        emitter.setNowrap(noWrapped);
//        emitter.setAllowInvalidURL(allowInvalidURL);
        emitter.setWrapArrays(wrapArrays);
//        if (namespaceMappingFile != null) {
//            emitter.setNStoPkg(namespaceMappingFile.toString());
//        }
//        emitter.setTimeout(timeout);
        emitter.setImplementationClassName(implementationClassName);

//        Authenticator.setDefault(new DefaultAuthenticator(username, password));
//        if (classpath != null) {
//            AntClassLoader cl = new AntClassLoader(
//                    getClass().getClassLoader(),
//                    getProject(),
//                    classpath,
//                    false);
//            log("Using CLASSPATH " + cl.getClasspath(),
//                    Project.MSG_VERBOSE);
//            ClassUtils.setDefaultClassLoader(cl);
//        }
        
        if (javaXmlTypeMappings != null && javaXmlTypeMappings.length > 0) {
            GeneratorFactory factory = emitter.getFactory();
            CustomizableBaseTypeMapping btm = new CustomizableBaseTypeMapping(factory.getBaseTypeMapping());
            for (int i=0; i<javaXmlTypeMappings.length; i++) {
                String xmlTypeName = javaXmlTypeMappings[i].getXmlType();
                if (xmlTypeName.length() == 0 || xmlTypeName.charAt(0) != '{') {
                    throw new MojoFailureException("Invalid XML type '" + xmlTypeName + "'");
                }
                int idx = xmlTypeName.indexOf('}', 1);
                if (idx == -1) {
                    throw new MojoFailureException("Invalid XML type '" + xmlTypeName + "'");
                }
                btm.addMapping(new QName(xmlTypeName.substring(1, idx), xmlTypeName.substring(idx+1)), javaXmlTypeMappings[i].getJavaType());
            }
            factory.setBaseTypeMapping(btm);
        }
        
        configureEmitter(emitter);
        
        getLog().info("Processing " + wsdlUrl);
        
        try {
            emitter.run(wsdlUrl);
        } catch (Exception ex) {
            throw new MojoFailureException("wsdl2java failed", ex);
        }
        
        addSourceRoot(project, getSourceOutputDirectory().getAbsolutePath());
    }
    
    protected abstract File getSourceOutputDirectory();
    protected abstract void configureEmitter(EmitterEx emitter);
    protected abstract void addSourceRoot(MavenProject project, String path);
}
