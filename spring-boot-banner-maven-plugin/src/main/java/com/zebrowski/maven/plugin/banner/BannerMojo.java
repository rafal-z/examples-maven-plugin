package com.zebrowski.maven.plugin.banner;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.ssl.TrustAllStrategy;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.util.EntityUtils;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;

@Mojo(name = "banner", defaultPhase = LifecyclePhase.GENERATE_RESOURCES, threadSafe = true, requiresOnline = true)
public class BannerMojo extends AbstractMojo {

    private static final String UTF_8 = StandardCharsets.UTF_8.name();

    @Parameter(property = "spring-boot-banner.text", required = true)
    private String text;

    @Parameter(property = "spring-boot-banner.font", defaultValue = "standard")
    private String font;

    @Parameter(property = "spring-boot-banner.filename", defaultValue = "banner.txt")
    private String filename;

    @Parameter(property = "spring-boot-banner.directory", defaultValue = "${project.build.outputDirectory}")
    private File directory;

    @Parameter(property = "spring-boot-banner.request", defaultValue = "https://devops.datenkollektiv.de/renderBannerTxt?text={text}&font={font}")
    private String request;


    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        getLog().info("Generate banner...");
        String uri = request;
        uri = replaceGetParam(uri, "{text}", text);
        uri = replaceGetParam(uri, "{font}", font);
        final String banner = getBannerQuery(uri);
//        saveBannerCommand(banner);
        getLog().info(banner);
    }

    private static String replaceGetParam(final String uri, final String search, final String replacement) {
        try {
            return StringUtils.replace(uri, search, URLEncoder.encode(replacement, UTF_8));
        } catch (UnsupportedEncodingException ex) {
            throw new RuntimeException(ex);
        }
    }

    private String getBannerQuery(final String uri) throws MojoExecutionException {
        getLog().info(uri);

        try (CloseableHttpClient httpClient = HttpClients.custom()
                .setSSLContext(new SSLContextBuilder().loadTrustMaterial(null, TrustAllStrategy.INSTANCE).build())
                .build()) {
            HttpGet httpGet = new HttpGet(uri);
            CloseableHttpResponse response = httpClient.execute(httpGet);
            return EntityUtils.toString(response.getEntity());
        } catch (KeyManagementException | NoSuchAlgorithmException | KeyStoreException | IOException ex) {
            throw new MojoExecutionException("Oops. The banner could not be generated.", ex);
        }
    }

    private void saveBannerCommand(final String banner) throws MojoExecutionException {
        directory.mkdirs();
        Path path = directory.toPath().resolve(filename);

        try {
            Files.write(path, banner.getBytes(UTF_8));
        } catch (IOException ex) {
            throw new MojoExecutionException("Oops. The file could not be saved.", ex);
        }

    }


}
