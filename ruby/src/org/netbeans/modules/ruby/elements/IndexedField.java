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
 * Contributor(s):
 *
 * Portions Copyrighted 2007 Sun Microsystems, Inc.
 */
package org.netbeans.modules.ruby.elements;

import org.netbeans.modules.csl.api.ElementKind;
import org.netbeans.modules.parsing.spi.indexing.support.IndexResult;
import org.netbeans.modules.ruby.RubyIndex;
import org.netbeans.modules.ruby.RubyType;
import org.openide.filesystems.FileObject;

/**
 * @author Tor Norbye
 */
public class IndexedField extends IndexedElement {

    private boolean smart;
    private String name;
    private boolean inherited;

    private IndexedField(String name, RubyIndex index, IndexResult result, String fqn,
        String clz, String require, String attributes, int flags, FileObject context) {
        super(index, result, fqn, clz, require, attributes, flags, context);
        this.name = name;
    }

    public static IndexedField create(RubyIndex index, String name, String fqn, String clz,
        IndexResult result, String require, String attributes, int flags, FileObject context) {
        IndexedField m =
            new IndexedField(name, index, result, fqn, clz, require, attributes, flags, context);

        return m;
    }

    public ElementKind getKind() {
        return ElementKind.FIELD;
    }
    
    @Override
    public String getSignature() {
        return fqn + "#@" + (isStatic() ? "@" : "") + name;
    }

    public String getName() {
        return name;
    }

    public boolean isSmart() {
        return smart;
    }
    
    public void setSmart(boolean smart) {
        this.smart = smart;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final IndexedField other = (IndexedField) obj;
        if (this.name != other.name && (this.name == null || !this.name.equals(other.name))) {
            return false;
        }
        if (this.fqn != other.fqn && (this.fqn == null || !this.fqn.equals(other.fqn))) {
            return false;
        }
        if (this.flags != other.flags) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 43 * hash + (this.name != null ? this.name.hashCode() : 0);
        hash = 43 * hash + (this.fqn != null ? this.fqn.hashCode() : 0);
        hash = 53 * hash + flags;
        return hash;
    }

    public boolean isInherited() {
        return inherited;
    }

    public void setInherited(boolean inherited) {
        this.inherited = inherited;
    }

    // For testsuite
    public static String decodeFlags(int flags) {
        return IndexedElement.decodeFlags(flags);
    }

    // For testsuite
    public static int stringToFlags(String string) {
        return IndexedElement.stringToFlags(string);
    }
}
