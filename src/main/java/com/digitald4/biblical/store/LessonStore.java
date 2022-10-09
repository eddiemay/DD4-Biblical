package com.digitald4.biblical.store;

import static com.google.common.collect.ImmutableList.toImmutableList;

import com.digitald4.biblical.model.Lesson;
import com.digitald4.biblical.model.Lesson.LessonVersion;
import com.digitald4.common.storage.DAO;
import com.digitald4.common.storage.GenericStore;
import com.digitald4.common.storage.Query;
import com.google.common.collect.ImmutableList;

import javax.inject.Inject;
import javax.inject.Provider;
import java.util.function.UnaryOperator;

public class LessonStore extends GenericStore<Lesson, Long> {

  @Inject
  public LessonStore(Provider<DAO> daoProvider) {
    super(Lesson.class, daoProvider);
  }

  public ImmutableList<Lesson> list(boolean allowDraft) {
    return list(Query.forList()).getItems().stream()
        .filter(lesson -> allowDraft || lesson.getLatestPublishedVersionId() > 0)
        .collect(toImmutableList());
  }

  private LessonVersion updateLatest(LessonVersion lessonVersion) {
    update(lessonVersion.getLessonId(), lesson -> {
      if (lessonVersion.isPublished()) {
        lesson.setTitle(lessonVersion.getTitle())
            .setLatestPublishedVersionId(lessonVersion.getId());
      }
      return lesson.setLatestVersionId(lessonVersion.getId());
    });

    return lessonVersion;
  }

  public static class LessonVersionStore extends GenericStore<LessonVersion, Long> {
    private final LessonStore lessonStore;

    @Inject
    public LessonVersionStore(Provider<DAO> daoProvider, LessonStore lessonStore) {
      super(LessonVersion.class, daoProvider);
      this.lessonStore = lessonStore;
    }

    public LessonVersion getLatest(long lessonId, boolean allowDraft) {
      Lesson lesson = lessonStore.get(lessonId);
      long latestId = allowDraft ? lesson.getLatestVersionId() : lesson.getLatestPublishedVersionId();
      if (latestId == 0) {
        return null;
      }

      return get(latestId);
    }

    @Override
    public LessonVersion create(LessonVersion lessonVersion) {
      if (lessonVersion.getLessonId() == 0) {
        lessonVersion.setLessonId(lessonStore.create(Lesson.create(lessonVersion)).getId());
      }
      return lessonStore.updateLatest(super.create(lessonVersion));
    }

    @Override
    public LessonVersion update(Long lessonId, UnaryOperator<LessonVersion> updater) {
      LessonVersion latest = getLatest(lessonId, true);
      if (latest == null) {
        return create(updater.apply(new LessonVersion().setLessonId(lessonId)));
      }

      return latest.isPublished()
          ? create(updater.apply(latest.setId(0).setPublished(false)))
          : lessonStore.updateLatest(super.update(latest.getId(), updater));
    }
  }
}
