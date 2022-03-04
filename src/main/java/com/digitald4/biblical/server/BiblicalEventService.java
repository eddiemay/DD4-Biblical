package com.digitald4.biblical.server;

import com.digitald4.biblical.model.BiblicalEvent;
import com.digitald4.biblical.store.BiblicalEventStore;
import com.digitald4.common.exception.DD4StorageException;
import com.digitald4.common.model.BasicUser;
import com.digitald4.common.server.service.EntityServiceImpl;
import com.digitald4.common.storage.QueryResult;
import com.digitald4.common.storage.SessionStore;
import com.google.api.server.spi.ServiceException;
import com.google.api.server.spi.config.*;
import com.google.common.collect.ImmutableList;

import javax.inject.Inject;

@Api(
    name = "biblicalEvents",
    version = "v1",
    namespace = @ApiNamespace(
        ownerDomain = "biblical.digitald4.com",
        ownerName = "biblical.digitald4.com"
    ),
    // [START_EXCLUDE]
    issuers = {
        @ApiIssuer(
            name = "firebase",
            issuer = "https://securetoken.google.com/biblical",
            jwksUri = "https://www.googleapis.com/robot/v1/metadata/x509/securetoken@system.gserviceaccount.com")
    }
    // [END_EXCLUDE]
)
public class BiblicalEventService extends EntityServiceImpl<BiblicalEvent> {
  private final BiblicalEventStore store;

  @Inject
  BiblicalEventService(BiblicalEventStore store, SessionStore<BasicUser> sessionStore) {
    super(store, sessionStore, true);
    this.store = store;
  }

  @ApiMethod(httpMethod = ApiMethod.HttpMethod.GET, path = "month/{month}")
  public ImmutableList<BiblicalEvent> listByMonth(@Named("month") int month) throws ServiceException {
    try {
      return store.getBiblicalEvents(month);
    } catch (DD4StorageException e) {
      throw new ServiceException(e.getErrorCode(), e);
    }
  }
}
