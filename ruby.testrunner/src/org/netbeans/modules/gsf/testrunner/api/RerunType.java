// XXX copied from gsf.testrunner 1.16:
package org.netbeans.modules.gsf.testrunner.api;
public enum RerunType {

    ALL("All"), CUSTOM("Custom"); //NOI18N

    private final String name;

    private RerunType(String name) {
        this.name = name;
    }

    /**
     * @return the name of the rerun type.
     */
    public String getName() {
        return name;
    }

}
