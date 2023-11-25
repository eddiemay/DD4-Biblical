package com.digitald4.biblical.server;

import com.digitald4.common.exception.DD4StorageException;
import com.digitald4.common.exception.DD4StorageException.ErrorCode;
import com.digitald4.common.model.DataFile;
import com.digitald4.common.server.APIConnector;
import com.digitald4.common.server.service.Empty;
import com.digitald4.common.storage.LoginResolver;
import com.digitald4.common.storage.Store;
import com.google.api.server.spi.config.Api;
import com.google.api.server.spi.config.ApiIssuer;
import com.google.api.server.spi.config.ApiMethod;
import com.google.api.server.spi.config.ApiMethod.HttpMethod;
import com.google.api.server.spi.config.ApiNamespace;
import com.google.api.server.spi.config.Named;
import com.google.api.server.spi.config.Nullable;
import java.io.IOException;
import java.io.InputStream;
import javax.inject.Inject;
import javax.inject.Provider;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Api(
    name = "files",
    version = "v1",
    namespace =
    @ApiNamespace(
        ownerDomain = "nbastats.digitald4.com",
        ownerName = "nbastats.digitald4.com"
    ),
    // [START_EXCLUDE]
    issuers = {
        @ApiIssuer(
            name = "firebase",
            issuer = "https://securetoken.google.com/fantasy-predictor",
            jwksUri =
                "https://www.googleapis.com/service_accounts/v1/metadata/x509/securetoken@system"
                    + ".gserviceaccount.com"
        )
    }
    // [END_EXCLUDE]
)
public class FileService extends com.digitald4.common.server.service.FileService {
  private static final String ISA_TILES_PREFIX = "tiles_isaiah_";

  private final APIConnector apiConnector;
  @Inject
  public FileService(
      Store<DataFile, String> dataFileStore,
      LoginResolver loginResolver,
      Provider<HttpServletRequest> requestProvider,
      Provider<HttpServletResponse> responseProvider,
      APIConnector apiConnector) {
    super(dataFileStore, loginResolver, requestProvider, responseProvider);
    this.apiConnector = apiConnector;
  }

  @Override
  @ApiMethod(httpMethod = HttpMethod.GET, path = "{fileName}")
  public Empty getFileContents(
      @Named("fileName") String fileName, @Nullable @Named("idToken") String idToken) {
    if (fileName.startsWith(ISA_TILES_PREFIX)) {
      try {
        String url =
            "http://tiles.imj.org.il/tiles/isaiah/" + fileName.substring(ISA_TILES_PREFIX.length());
        InputStream inputStream = apiConnector.getInputStream("GET", url, null);
        byte[] bytes = new byte[16 * 1024 * 1024];
        int totalRead = 0;
        int read = 0;
        while ((read = inputStream.read(bytes, totalRead, 4096)) > 0) {
          totalRead += read;
        }
        inputStream.close();

        HttpServletResponse response = responseProvider.get();
        response.setContentType("application/jpg");
        response.setHeader("Cache-Control", "max-age=31536000");
        response.setContentLength(totalRead);
        response.getOutputStream().write(bytes, 0, totalRead);
        return null;
      } catch (IOException ioe) {
        throw new DD4StorageException("Error fetching file", ioe, ErrorCode.INTERNAL_SERVER_ERROR);
      }
    }
    return super.getFileContents(fileName, idToken);
  }
}
