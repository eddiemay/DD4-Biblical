package com.digitald4.biblical.server;

import com.digitald4.biblical.model.BiblicalEvent;
import com.digitald4.biblical.store.BiblicalEventStore;
import com.digitald4.common.exception.DD4StorageException;
import com.digitald4.common.server.service.EntityServiceBulkImpl;
import com.digitald4.common.storage.LoginResolver;
import com.google.api.server.spi.ServiceException;
import com.google.api.server.spi.config.*;
import com.google.common.collect.ImmutableList;

import javax.inject.Inject;
import java.util.concurrent.atomic.AtomicInteger;

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
public class BiblicalEventService extends EntityServiceBulkImpl<Long, BiblicalEvent> {
  private final BiblicalEventStore store;

  @Inject
  BiblicalEventService(BiblicalEventStore store, LoginResolver loginResolver) {
    super(store, loginResolver);
    this.store = store;
  }

  @ApiMethod(httpMethod = ApiMethod.HttpMethod.GET, path = "calendar_events")
  public ImmutableList<BiblicalEvent> listCalendarEvents(@Named("month") int month) throws ServiceException {
    try {
      return store.getBiblicalEvents(month);
    } catch (DD4StorageException e) {
      throw new ServiceException(e.getErrorCode(), e);
    }
  }

  @ApiMethod(httpMethod = ApiMethod.HttpMethod.GET, path = "timeline_events")
  public ImmutableList<BiblicalEvent> listTimelineEvents(
      @Nullable @Named("startYear") int startYear, @Nullable @Named("endYear") int endYear) throws ServiceException {
    try {
      return store.getBiblicalEvents(startYear, endYear);
    } catch (DD4StorageException e) {
      throw new ServiceException(e.getErrorCode(), e);
    }
  }

  @ApiMethod(httpMethod = ApiMethod.HttpMethod.GET, path = "all")
  public ImmutableList<BiblicalEvent> getAll(@Nullable @Named("idToken") String idToken) throws ServiceException {
    try {
      resolveLogin(idToken);
      return store.getAll();
    } catch (DD4StorageException e) {
      throw new ServiceException(e.getErrorCode(), e);
    }
  }

  @ApiMethod(httpMethod = ApiMethod.HttpMethod.GET, path = "migrate")
  public AtomicInteger migrate(@Nullable @Named("idToken") String idToken) throws ServiceException {
    try {
      resolveLogin(idToken);
      return new AtomicInteger(store.migrate().size());
    } catch (DD4StorageException e) {
      throw new ServiceException(e.getErrorCode(), e);
    }
  }
}
