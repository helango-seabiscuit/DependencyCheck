/*
 * This file is part of dependency-check-core.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Copyright (c) 2016 Bianca Jiang. All Rights Reserved.
 */
package org.owasp.dependencycheck.analyzer;

import java.io.File;
import java.io.FilenameFilter;

import org.owasp.dependencycheck.Engine;
import org.owasp.dependencycheck.analyzer.exception.AnalysisException;
import org.owasp.dependencycheck.dependency.Dependency;

/**
 * This analyzer accepts the fully resolved .gemspec created by the Ruby bundler (http://bundler.io)
 * for better evidence results. It also tries to resolve the dependency packagePath 
 * to where the gem is actually installed. Then during {@link AnalysisPhase.PRE_FINDING_ANALYSIS}
 * {@link DependencyBundlingAnalyzer} will merge two .gemspec dependencies together if 
 * <code>Dependency.getPackagePath()</code> are the same.
 * 
 * Ruby bundler creates new .gemspec files under a folder called "specifications" at deploy time, 
 * in addition to the original .gemspec files from source. The bundler generated 
 * .gemspec files always contain fully resolved attributes thus provide more accurate 
 * evidences, whereas the original .gemspec from source often contain variables for attributes
 * that can't be used for evidences.
 * 
 * Note this analyzer share the same {@link Settings.KEYS.ANALYZER_RUBY_GEMSPEC_ENABLED} as
 * {@link RubyGemspecAnalyzer}, so it will enabled/disabled with {@link RubyGemspecAnalyzer}.
 *
 * @author Bianca Jiang (biancajiang@gmail.com)
 */
public class RubyBundlerAnalyzer extends RubyGemspecAnalyzer {

    /**
     * The name of the analyzer.
     */
    private static final String ANALYZER_NAME = "Ruby Bundler Analyzer";
    
	//Folder name that contains .gemspec files created by "bundle install"
	private static final String SPECIFICATIONS = "specifications";
	
	//Folder name that contains the gems by "bundle install"
	private static final String GEMS = "gems";

    /**
     * Only accept *.gemspec files generated by "bundle install --deployment" under "specifications" folder.
     */
	@Override
    public boolean accept(File pathname) {
		
        boolean accepted = super.accept(pathname);
        if(accepted == true) {
	        File parentDir = pathname.getParentFile();
	        accepted = parentDir != null && parentDir.exists() && parentDir.getName().equals(SPECIFICATIONS);
        }
        
        return accepted;
    }
	
	@Override
    protected void analyzeFileType(Dependency dependency, Engine engine)
            throws AnalysisException {
        super.analyzeFileType(dependency, engine);
        
        //find the corresponding gem folder for this .gemspec stub by "bundle install --deployment"
        File gemspecFile = dependency.getActualFile();
        String gemFileName = gemspecFile.getName();
        final String gemName = gemFileName.substring(0, gemFileName.lastIndexOf(".gemspec"));
        File specificationsDir = gemspecFile.getParentFile();
    	if(specificationsDir != null && specificationsDir.getName().equals(SPECIFICATIONS) && specificationsDir.exists()) {
    		File parentDir = specificationsDir.getParentFile();
    		if(parentDir != null && parentDir.exists()) {
    			File gemsDir = new File(parentDir, GEMS);
    			if(gemsDir != null && gemsDir.exists()) {
		    		File[] matchingFiles = gemsDir.listFiles(new FilenameFilter() {
		    		    public boolean accept(File dir, String name) {
		    		        return name.equals(gemName);
		    		    }
		    		});
		    		
		    		if(matchingFiles.length > 0) {
		    			String gemPath = matchingFiles[0].getAbsolutePath();
		    			if(gemPath != null)
		    				dependency.setPackagePath(gemPath);
		    		}
    			}
    		}
    	}
	}
}
