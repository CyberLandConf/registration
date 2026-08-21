package de.jugda.registration.api.authenticated;

import de.jugda.registration.TenantContext;
import io.quarkus.oidc.IdToken;
import io.quarkus.qute.Location;
import io.quarkus.qute.Template;
import io.quarkus.qute.TemplateInstance;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.jwt.JsonWebToken;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayDeque;
import java.util.Deque;

@Path("/admin/{tenant}/logs")
@RolesAllowed("admin")
public class AdminLogViewerResource {

    @ConfigProperty(name = "quarkus.log.file.path", defaultValue = "data/app.log")
    String logFilePath;

    @Inject
    TenantContext tenantCtx;

    @Inject
    @IdToken
    JsonWebToken idToken;

    @Location("admin/logs")
    Template logsTemplate;

    @GET
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance logsPage() {
        return logsTemplate
            .data("tenant", tenantCtx.getTenant())
            .data("id", idToken);
    }

    @GET
    @Path("tail")
    @Produces(MediaType.TEXT_PLAIN)
    public String tail(@QueryParam("lines") Integer linesParam) throws IOException {
        int maxLines = linesParam != null ? Math.min(linesParam, 3000) : 2000;
        return tailFile(logFilePath, maxLines);
    }

    private String tailFile(String path, int maxLines) throws IOException {
        Deque<String> ring = new ArrayDeque<>(maxLines);
        try (RandomAccessFile raf = new RandomAccessFile(path, "r")) {
            long pointer = raf.length();
            StringBuilder line = new StringBuilder();

            for (long fp = pointer - 1; fp >= 0; fp--) {
                raf.seek(fp);
                int c = raf.read();
                if (c == '\n') {
                    if (!line.isEmpty()) {
                        ring.addFirst(line.reverse().toString());
                        line.setLength(0);
                        if (ring.size() >= maxLines) break;
                    }
                } else if (c != '\r') {
                    line.append((char) c);
                }
            }
            if (!line.isEmpty() && ring.size() < maxLines) {
                ring.addFirst(line.reverse().toString());
            }
        }
        return String.join("\n", ring);
    }
}
