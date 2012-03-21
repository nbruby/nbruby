/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2010 Oracle and/or its affiliates. All rights reserved.
 *
 * Oracle and Java are registered trademarks of Oracle and/or its affiliates.
 * Other names may be trademarks of their respective owners.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common
 * Development and Distribution License("CDDL") (collectively, the
 * "License"). You may not use this file except in compliance with the
 * License. You can obtain a copy of the License at
 * http://www.netbeans.org/cddl-gplv2.html
 * or nbbuild/licenses/CDDL-GPL-2-CP. See the License for the
 * specific language governing permissions and limitations under the
 * License.  When distributing the software, include this License Header
 * Notice in each file and include the License file at
 * nbbuild/licenses/CDDL-GPL-2-CP.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the GPL Version 2 section of the License file that
 * accompanied this code. If applicable, add the following below the
 * License Header, with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 * 
 * If you wish your version of this file to be governed by only the CDDL
 * or only the GPL Version 2, indicate your decision by adding
 * "[Contributor] elects to include this software in this distribution
 * under the [CDDL or GPL Version 2] license." If you do not indicate a
 * single choice of license, a recipient has the option to distribute
 * your version of this file under either the CDDL, the GPL Version 2 or
 * to extend the choice of license to its licensees as provided above.
 * However, if you add GPL Version 2 code and therefore, elected the GPL
 * Version 2 license, then the option applies only if the new code is
 * made subject to such option by the copyright holder.
 * 
 * Contributor(s):
 * 
 * Portions Copyrighted 2008 Sun Microsystems, Inc.
 */

package org.netbeans.modules.ruby.railsprojects.database;

import org.netbeans.modules.ruby.railsprojects.RailsProject;

/**
 * Handles the database configuration for a Rails project.
 *
 * @author Erno Mononen
 */
public abstract class RailsDatabaseConfiguration {

    /**
     * Gets the database parameter passed to the rails command, i.e. the name 
     * of the Rails database adapter to be used.
     * 
     * @return the parameter for the Rails generator or <code>null</code> if
     * the Rails generator should not be used for generating database configuration
     * for this.
     */
    public abstract String railsGenerationParam();
    
    /**
     * Edits the database config file (database.yml) of the given <code>project</code>
     * as required by this configuration, and in case of JDBC connections, 
     * possibly adds a reference to the required 
     * driver jar files to the properties of the <code>project</code>.
     * 
     * @param projectDir the project whose <code>database.yml</code> is 
     * to be edited.
     */
    public abstract void editConfig(RailsProject project);
    
    /**
     * Gets the JDBC adapter configuration info for presenting 
     * this configuration as a JDBC adapter in database.yml.
     * 
     * @return the info or <code>null</code>.
     */
    abstract JdbcInfo getJdbcInfo();
    
    /**
     * Gets the display name for this configuration, typically 
     * the name of the adapter. May return null.
     * 
     * @return the display name or null.
     */
    public abstract String getDisplayName();

    /**
     * Gets the preferred <strong>development</strong> database name
     * of this adapter, i.e. the name that the adapter will use for the
     * project if the user doesn't specify otherwise.
     *
     * @param projectName the name of the project.
     * @return the default development database name.
     */
    public abstract String getDatabaseName(String projectName);

    /**
     * Guesses a name for the test database, possibly based on the given 
     * <code>developmentDbName</code>. Useful for the case when the user
     * has specified only the development database.
     * 
     * @param developmentDbName the name of the development database.
     * @return a name for the test database
     */
    abstract String getTestDatabaseName(String developmentDbName);

    /**
     * Guesses a name for the production database, possibly based on the given
     * <code>developmentDbName</code>. Useful for the case when the user
     * has specified only the development database.
     *
     * @param developmentDbName the name of the development database.
     * @return a name for the production database
     */
    abstract String getProductionDatabaseName(String developmentDbName);

    /**
     * Checks whether the configuration can only work with JDBC.
     *
     * @return true if the adapter can be used only with JDBC.
     */
    public boolean requiresJdbc() {
        return false;
    }
}
