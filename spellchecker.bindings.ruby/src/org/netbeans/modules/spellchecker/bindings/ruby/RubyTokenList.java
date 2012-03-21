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
 * The Original Software is NetBeans. The Initial Developer of the Original
 * Software is Sun Microsystems, Inc. Portions Copyright 1997-2006 Sun
 * Microsystems, Inc. All Rights Reserved.
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
 */
package org.netbeans.modules.spellchecker.bindings.ruby;

import javax.swing.text.BadLocationException;
import org.netbeans.api.lexer.TokenId;
import org.netbeans.api.lexer.TokenSequence;
import org.netbeans.editor.BaseDocument;
import org.netbeans.editor.Utilities;
import org.netbeans.modules.ruby.lexer.RubyCommentTokenId;
import org.netbeans.modules.ruby.lexer.RubyTokenId;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;

/**
 * Tokenize Ruby text for spell checking
 *
 * @todo Check spelling in documentation sections
 * @todo Suppress spelling checks on :rdoc: modifiers
 * @todo Remove surrounding +, _, * on spelling words
 * @todo Spell check string literals?
 * @todo Spell check constant names and method names?
 *
 *
 *
 * @author Tor Norbye
 */
public class RubyTokenList extends AbstractRubyTokenList {


    private boolean hidden = false;

    /** Creates a new instance of RubyTokenList */
    public RubyTokenList(BaseDocument doc) {
        super(doc);
    }

    @Override
    public void setStartOffset(int offset) {
        super.setStartOffset (offset);
        FileObject fileObject = FileUtil.getConfigFile ("Spellcheckers/Ruby");
        Boolean b = (Boolean) fileObject.getAttribute ("Hidden");
        hidden = Boolean.TRUE.equals (b);
    }

    /** Given a sequence of Ruby tokens, return the next span of eligible comments */
    @Override
    protected int[] findNextSpellSpan(TokenSequence<? extends TokenId> ts, int offset) throws BadLocationException {
        if (ts == null || hidden) {
            return new int[]{-1, -1};
        }

        int diff = ts.move(offset);

        while (ts.moveNext()) {
            TokenId id = ts.token().id();
            if ((id == RubyTokenId.LINE_COMMENT || id == RubyTokenId.DOCUMENTATION) && !isPreformatted(ts.offset(), id != RubyTokenId.DOCUMENTATION)) {
                TokenSequence<? extends TokenId> t = ts.embedded(RubyCommentTokenId.language());
                if (t == null) {
                    return new int[]{ts.offset(), ts.offset() + ts.token().length()};
                } else {
                    t.move(offset);
                    while (t.moveNext()) {
                        id = t.token().id();
                        if ((id == RubyCommentTokenId.COMMENT_TEXT || id == RubyCommentTokenId.COMMENT_BOLD || id == RubyCommentTokenId.COMMENT_ITALIC) && !isPreformatted(t.offset(), id != RubyTokenId.DOCUMENTATION)) {
                            return new int[]{t.offset(), t.offset() + t.token().length()};
                        }
                    }
                }
            }
        }

        return new int[]{-1, -1};
    }

    private boolean isPreformatted(int offset, boolean isComment) throws BadLocationException {
        int lineBegin = Utilities.getRowFirstNonWhite(doc, offset);
        if (lineBegin == -1) {
            return false;
        }

        // See if this comment is indented more than two chars
        String line = doc.getText(lineBegin, Math.min(Utilities.getRowEnd(doc, offset), lineBegin + 5) - lineBegin);
        return isComment ? line.startsWith("#  ") : line.startsWith("  "); // NOI18N
    }
}