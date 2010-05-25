/*
 * @(#)NarMojo.java
 * 
 * Copyright 2010 MBARI
 *
 * Licensed under the GNU LESSER GENERAL PUBLIC LICENSE, Version 2.1
 * (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.gnu.org/copyleft/lesser.html
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.mbari.aved.classifier.NarMojo;

//~--- non-JDK imports --------------------------------------------------------
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;

import org.codehaus.plexus.archiver.ArchiverException;
import org.codehaus.plexus.archiver.UnArchiver;
import org.codehaus.plexus.archiver.manager.ArchiverManager;
import org.codehaus.plexus.archiver.manager.NoSuchArchiverException;
import org.codehaus.plexus.util.FileUtils;

import org.apache.maven.plugin.nar.AbstractNarMojo;
import org.apache.maven.plugin.nar.Linker;
import org.apache.maven.plugin.nar.NarManager;
import org.apache.maven.plugin.nar.NarUtil;

//~--- JDK imports ------------------------------------------------------------

import java.io.File;
import java.io.IOException;

import java.util.Iterator;
import java.util.List;

/**
 * This mojo will unpack shared libraries into the
 * target directory for inclusion in a jar.
 *
 * Note that the native way to nars to be unpacked is
 * in the local respository.
 *
 * This was designed for use during the bundle phase
 * of building AVED Editor
 *
 * @goal unpack
 * @phase process-resources
 * @requiresDependencyResolution compile
 */
public class NarMojo extends AbstractMojo {

    /**
     * The Architecture for picking up swig, Some choices are: "x86", "i386",
     * "amd64", "ppc", "sparc", ... Defaults to ${os.arch}
     *
     * @parameter expression="${os.arch}"
     * @required
     */
    private String architecture;
    /**
     * To look up Archiver/UnArchiver implementations
     *
     * @component role="org.codehaus.plexus.archiver.manager.ArchiverManager"
     * @required
     */
    private ArchiverManager archiverManager;
    /**
     * @parameter expression="${localRepository}"
     * @required
     * @readonly
     */
    private ArtifactRepository localRepository;
    private Log log;
    private NarManager narManager;
    /**
     * The Operating System for picking up swig. Some choices are: "Windows",
     * "Linux", "MacOSX", "SunOS", ... Defaults to a derived value from
     * ${os.name}
     *
     * @parameter expression=""
     */
    private String os;
    /**
     * @parameter expression="${project}"
     * @required
     * @readonly
     */
    private MavenProject project;

    public void execute() throws MojoExecutionException, MojoFailureException {
        os = NarUtil.getOS(os);

        // FIXME, should have some function in NarUtil
        Linker linker = new Linker("g++");

        narManager = new NarManager(getLog(), localRepository, project, architecture, os, linker);

        String aol = architecture + "-" + os + "-g++";
        String classifier = aol;

        System.out.println("Classifier: " + classifier);

        List narArtifacts = narManager.getNarDependencies("compile");

        // List libs = narManager.getAttachedNarDependencies(narArtifacts, aol, "jni");

        System.out.println("Unpacking libraries");
        this.unpackAttachedNars(narArtifacts, narManager, archiverManager, os);
    }

    private void unpackNar(ArchiverManager manager, File file, File location) throws MojoExecutionException, IOException {
        try {
            UnArchiver unArchiver;

            unArchiver = manager.getUnArchiver(AbstractNarMojo.NAR_ROLE_HINT);
            unArchiver.setSourceFile(file);
            unArchiver.setDestDirectory(location);
            unArchiver.extract();
        } catch (NoSuchArchiverException e) {
            throw new MojoExecutionException("Error unpacking file: " + file + " to: " + location, e);
        } catch (ArchiverException e) {
            throw new MojoExecutionException("Error unpacking file: " + file + " to: " + location, e);
        }
    }

    public void unpackAttachedNars(List /* <NarArtifacts> */ narArtifacts, NarManager narManager,
            ArchiverManager manager, String os)
            throws MojoExecutionException, MojoFailureException {

        // FIXME, kludge to get to download the -noarch, based on classifier
        List dependencies = narManager.getAttachedNarDependencies(narArtifacts, os);

        for (Iterator i = dependencies.iterator(); i.hasNext();) {
            Artifact dependency = (Artifact) i.next();
            File file = narManager.getNarFile(dependency);
            File narLocation = new File(project.getBasedir().toString() + "/target");
            File flagFile = new File(narLocation,
                    FileUtils.basename(file.getPath(), "." + AbstractNarMojo.NAR_EXTENSION)
                    + ".flag");

            System.out.println("Unpack " + file + " to" + narLocation);

            boolean process = false;

            if (!narLocation.exists()) {
                narLocation.mkdirs();
                process = true;
            } else if (!flagFile.exists()) {
                process = true;
            } else if (file.lastModified() > flagFile.lastModified()) {
                process = true;
            }

            if (process) {
                try {
                    unpackNar(manager, file, narLocation);
                    NarUtil.makeExecutable(new File(narLocation, "bin/"), log);
                    FileUtils.fileDelete(flagFile.getPath());
                    FileUtils.fileWrite(flagFile.getPath(), ""); 
                } catch (MojoExecutionException e) {
                    throw e;
                } catch (IOException e) {
                    log.warn("Cannot create flag file: " + flagFile.getPath() + e.getMessage());
                }
            }
        }
    }
}
