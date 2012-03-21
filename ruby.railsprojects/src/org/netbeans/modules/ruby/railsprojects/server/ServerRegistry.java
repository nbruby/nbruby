
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
package org.netbeans.modules.ruby.railsprojects.server;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyVetoException;
import java.beans.VetoableChangeListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.netbeans.api.ruby.platform.RubyPlatform;
import org.netbeans.api.ruby.platform.RubyPlatformManager;
import org.netbeans.modules.ruby.platform.gems.GemInfo;
import org.netbeans.modules.ruby.platform.gems.GemManager;
import org.netbeans.modules.ruby.railsprojects.server.spi.RubyInstance;
import org.netbeans.modules.ruby.railsprojects.server.spi.RubyInstanceProvider;
import org.openide.util.lookup.Lookups;

/**
 * A server registry for servers with Ruby capabilities.
 *
 * TODO: a work in progess. Need to be better integrated with RubyInstanceProvider,
 * possibly implement an instance provider for WEBrick/Mongrel instead of 
 * handling them here.
 * 
 * @author peterw99, Erno Mononen, Michal Papis
 */
public class ServerRegistry implements VetoableChangeListener {

    private static ServerRegistry defaultRegistry;

    /**
     * Switch for enabling support for Phusion Passenger.
     */
    private static boolean ENABLE_PASSENGER = Boolean.getBoolean("passenger.support"); //NOI18N

    private ServerRegistry() {
    }

    public synchronized static ServerRegistry getDefault() {
        if (defaultRegistry == null) {
            defaultRegistry = new ServerRegistry();
            RubyPlatformManager.addVetoableChangeListener(defaultRegistry);
        }
        return defaultRegistry;
    }

    public List<RubyInstance> getServers() {
        List<RubyInstance> result = new ArrayList<RubyInstance>();
        // makes GF the default server
        for (RubyInstanceProvider provider : Lookups.forPath("Servers/Ruby").lookupAll(RubyInstanceProvider.class)) {
            result.addAll(provider.getInstances());
        }
        result.addAll(getRubyServers());
        return result;
    }

    List<RubyInstance> getServers(RubyPlatform platform) {
        List<RubyInstance> result = new ArrayList<RubyInstance>();
        for (RubyInstance each : getServers()) {
            if (each.isPlatformSupported(platform)) {
                result.add(each);
            }
        }
        return result;
    }

    List<RubyServer> getRubyServers() {
        List<RubyServer> result = new ArrayList<RubyServer>();
        for (RubyPlatform each : RubyPlatformManager.getPlatforms()) {
            result.addAll(RubyServerFactory.getInstance(each).getServers());
        }
        return result;
    }

    public RubyInstance getServer(String serverId, RubyPlatform platform) {

        for (RubyInstanceProvider provider : Lookups.forPath("Servers/Ruby").lookupAll(RubyInstanceProvider.class)) {
            RubyInstance instance = provider.getInstance(serverId);
            if (instance != null && instance.isPlatformSupported(platform)) {
                return instance;
            }
        }

        for (RubyServer each : RubyServerFactory.getInstance(platform).getServers()) {
            if (each.getServerUri().equals(serverId) && each.isPlatformSupported(platform)) {
                return each;
            }
        }
        return null;

    }

    public void vetoableChange(PropertyChangeEvent evt) throws PropertyVetoException {
        if (evt.getPropertyName().equals("platforms")) { //NOI18N
            ServerInstanceProviderImpl.getInstance().fireServersChanged();
        }
    }

    /**
     * A factory for Mongrel and WEBrick instances for a Ruby platform. Takes care 
     * of caching the instances and reinitializing 
     * the server list when there are changes in the gems of the platform.
     */
    private static class RubyServerFactory implements PropertyChangeListener {

        private static final Map<RubyPlatform, RubyServerFactory> instances = new HashMap<RubyPlatform, ServerRegistry.RubyServerFactory>();
        private final RubyPlatform platform;
        private final List<RubyServer> servers = new ArrayList<RubyServer>();

        private RubyServerFactory(RubyPlatform platform) {
            this.platform = platform;
        }

        public static synchronized RubyServerFactory getInstance(RubyPlatform platform) {
            RubyServerFactory existing = instances.get(platform);
            if (existing != null) {
                return existing;
            }
            RubyServerFactory result = new RubyServerFactory(platform);
            result.initGlassFish();
            result.initTrinidad();
            result.initMongrel();
            result.initWEBrick();
            if (ENABLE_PASSENGER) {
                result.initPassenger();
            }
            platform.addPropertyChangeListener(result);
            instances.put(platform, result);
            return result;
        }

        public List<RubyServer> getServers() {
            Collections.sort(servers, new ServerComparator());
            return Collections.<RubyServer>unmodifiableList(servers);
        }

        private RubyServer createInstance(Class clazz, GemInfo gemInfo) {
            if (clazz == Trinidad.class) {
                return new Trinidad(platform, gemInfo);
            } else if (clazz == GlassFishGem.class) {
                return new GlassFishGem(platform, gemInfo);
            } else if (clazz == Mongrel.class) {
                return new Mongrel(platform, gemInfo.getVersion());
            } else if (clazz == Passenger.class) {
                return new Passenger(platform, gemInfo.getVersion());
            }
            return null;
        }

        private void initServer(Class clazz, String gemName) {
            GemManager gemManager = platform.getGemManager();
            if (gemManager == null) {
                return;
            }

            List<GemInfo> versions = gemManager.getVersions(gemName);
            GemInfo gemInfo = versions.isEmpty() ? null : versions.get(0);
            if (gemInfo == null) {
                // remove all glassfish from gems
                for (Iterator<RubyServer> it = servers.iterator(); it.hasNext();) {
                    if (it.next().getClass() == clazz) {
                        it.remove();
                    }
                }
                return;

            }

            RubyServer candidate = createInstance(clazz, gemInfo);
            if (!servers.contains(candidate)) {
                servers.add(candidate);
            }
        }

        private void initGlassFish() {
            if (platform.isJRuby()) {
                initServer(GlassFishGem.class, GlassFishGem.GEM_NAME);
            }
        }

        private void initTrinidad() {
            if (platform.isJRuby()) {
                initServer(Trinidad.class, Trinidad.GEM_NAME);
            }
        }

        private void initMongrel() {
            initServer(Mongrel.class, Mongrel.GEM_NAME);
        }

        private void initWEBrick() {
            WEBrick candidate = new WEBrick(platform);
            if (!servers.contains(candidate)) {
                servers.add(candidate);
            }
        }

        private void initPassenger() {
            initServer(Passenger.class, Passenger.GEM_NAME);
        }

        public void propertyChange(PropertyChangeEvent evt) {
            if (evt.getPropertyName().equals("gems")) { //NOI18N
                initGlassFish();
                initMongrel();
                initWEBrick();
                ServerInstanceProviderImpl.getInstance().fireServersChanged();
            }
        }
    }

    static class ServerComparator implements Comparator<RubyServer> {

        public int compare(RubyServer o1, RubyServer o2) {
            if (o1.getClass().equals(o2.getClass())) {
                return o2.getDisplayName().compareTo(o1.getDisplayName());
            }
            if (o1 instanceof GlassFishGem) {
                return -1;
            }
            if (o2 instanceof GlassFishGem) {
                return 1;
            }
            if (o1 instanceof Mongrel) {
                return -1;
            }
            return 1;
        }
    }

}
