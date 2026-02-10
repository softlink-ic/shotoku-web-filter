/******************************************************************************
 * JBoss, a division of Red Hat                                               *
 * Copyright 2006, Red Hat Middleware, LLC, and individual                    *
 * contributors as indicated by the @authors tag. See the                     *
 * copyright.txt in the distribution for a full listing of                    *
 * individual contributors.                                                   *
 *                                                                            *
 * This is free software; you can redistribute it and/or modify it            *
 * under the terms of the GNU Lesser General Public License as                *
 * published by the Free Software Foundation; either version 2.1 of           *
 * the License, or (at your option) any later version.                        *
 *                                                                            *
 * This software is distributed in the hope that it will be useful,           *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of             *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU           *
 * Lesser General Public License for more details.                            *
 *                                                                            *
 * You should have received a copy of the GNU Lesser General Public           *
 * License along with this software; if not, write to the Free                *
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA         *
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.                   *
 ******************************************************************************/

/*  Example configuration in web.xml:
<filter>
   <filter-name>ResourcesFilter</filter-name>
   <filter-class>net.softlink.development.DevelopmentResourcesFilter</filter-class>
   <init-param>
      <param-name>enabled</param-name>
      <param-value>${development}</param-value>
   </init-param>
   <init-param>
      <param-name>filterVariables</param-name>
      <param-value>pom.version=1.0.0-SNAPSHOT,project.name=MyProject,primefaces.version=${primefaces.version}</param-value>
   </init-param>
</filter>

<filter-mapping>
   <filter-name>ResourcesFilter</filter-name>
   <url-pattern>/*</url-pattern>
   <dispatcher>REQUEST</dispatcher>
   <dispatcher>INCLUDE</dispatcher>
   <dispatcher>FORWARD</dispatcher>
</filter-mapping>

*/
package net.softlink.development;

import org.apache.commons.io.FilenameUtils;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Logger;

/**
 * A filter, which reads resources from the filesystem and makes them visible to the
 * application as deployed files --- useful for development.
 *
 * @author <a href="mailto:adam@warski.org">Adam Warski</a>
 */
public class DevelopmentResourceFilter implements Filter {
    private final static Logger logger = Logger.getLogger(DevelopmentResourceFilter.class.getName());

    private static final String SRC_DIR = "src/main/webapp";

    /**
     * Whether the filter is enabled; if not, it simply passes the request through.
     */
    boolean enabled = false;

    /**
     * A map of Maven POM variables to replace in .html files (e.g. ${pom.version}), which would normally be replaced during the build process. <br>
     * Format is key=value,key=value,... (e.g. pom.version=1.0.0-SNAPSHOT,project.name=MyProject,primefaces.version=${primefaces.version})
     */
    Map<String, String> filterVariables = new HashMap<>();

    /**
     * Base path to a directory where files will be copied; it's a subdirectory of a deployment directory created by the app server.
     */
    private String destBasePath;

    /**
     * A set of <code>java.lang.String</code>s, which are extensions, that are filtered.
     */
    private Set<String> extensions = new HashSet<>(Arrays.asList("jsp", "css", "html", "htm", "gif", "jpg", "jpeg", "png", "txt", "xhtml", "js"));

    public void init(FilterConfig conf) {
        destBasePath = conf.getServletContext().getRealPath("");

        enabled = Boolean.parseBoolean(conf.getInitParameter("enabled"));
        String paramString = conf.getInitParameter("filterVariables");
        if (paramString != null) {
            String[] pairs = paramString.split(",");
            for (String pair : pairs) {
                String[] keyValue = pair.split("=");
                if (keyValue.length == 2) {
                    filterVariables.put(keyValue[0].trim(), keyValue[1].trim());
                }
            }
        }
    }

    public void doFilter(ServletRequest request, ServletResponse response,
        FilterChain chain) throws IOException, ServletException {

        if (!enabled) {
            chain.doFilter(request, response);
            return;
        }

        if (request instanceof HttpServletRequest) {
            HttpServletRequest httpRequest = (HttpServletRequest) request;

            /* Getting the name of the requested resource; first checking if
             * it is an included, then forwarded resource. Finally, checking
             * the request uri itself. */
            String requestedResource = Optional.ofNullable(httpRequest.getAttribute("javax.servlet.include.servlet_path"))
                .map(Object::toString)
                .orElseGet(httpRequest::getServletPath);

            // Copying the file from the source directory to the deployment directory
            Path srcPath = Paths.get(SRC_DIR + requestedResource);
            Path destPath = Paths.get(destBasePath + requestedResource);
            if (extensions.contains(FilenameUtils.getExtension(requestedResource)) && Files.exists(srcPath)
                && (!Files.exists(destPath) || Files.getLastModifiedTime(srcPath).toMillis() > Files.getLastModifiedTime(destPath).toMillis())) {
                Files.copy(srcPath, destPath, StandardCopyOption.REPLACE_EXISTING);

                // replace "${pom.version}" and other relevant attributes with the actual version in .html files
                if (requestedResource.endsWith(".html")) {
                    String content = new String(Files.readAllBytes(destPath));
                    for (Map.Entry<String, String> entry : filterVariables.entrySet()) {
                        content = content.replace("${" + entry.getKey() + "}", entry.getValue());
                    }
                    Files.write(destPath, content.getBytes());
                }
            }
        }

        chain.doFilter(request, response);
    }

    public void destroy() {

    }
}
