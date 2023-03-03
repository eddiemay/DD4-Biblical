package com.digitald4.biblical.server;

import com.digitald4.biblical.model.Lesson;
import com.digitald4.biblical.model.Lesson.LessonVersion;
import com.digitald4.biblical.store.LessonStore;
import com.digitald4.biblical.store.LessonStore.LessonVersionStore;
import com.digitald4.common.exception.DD4StorageException;
import com.digitald4.common.server.service.EntityServiceImpl;
import com.digitald4.common.storage.LoginResolver;
import com.google.api.server.spi.ServiceException;
import com.google.api.server.spi.config.*;
import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;

@Api(
    name = "lessons",
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
public class LessonService extends EntityServiceImpl<LessonVersion, Long> {
  private final LessonStore lessonStore;
  private final LessonVersionStore lessonVersionStore;

  @Inject
  LessonService(
      LessonStore lessonStore, LessonVersionStore lessonVersionStore, LoginResolver loginResolver) {
    super(lessonVersionStore, loginResolver);
    this.lessonStore = lessonStore;
    this.lessonVersionStore = lessonVersionStore;
  }

  @ApiMethod(httpMethod = ApiMethod.HttpMethod.GET, path = "lessons")
  public ImmutableList<Lesson> listLessons(@Named("allowDraft") boolean allowDraft) throws ServiceException {
    try {
      return lessonStore.list(allowDraft);
    } catch (DD4StorageException e) {
      throw new ServiceException(e.getErrorCode(), e);
    }
  }

  @ApiMethod(httpMethod = ApiMethod.HttpMethod.GET, path = "latest")
  public LessonVersion getLatest(
      @Named("id") long lessonId, @Named("allowDraft") boolean allowDraft) throws ServiceException {
    try {
      return lessonVersionStore.getLatest(lessonId, allowDraft);
    } catch (DD4StorageException e) {
      throw new ServiceException(e.getErrorCode(), e);
    }
  }
}
