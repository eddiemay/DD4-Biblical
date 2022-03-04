package com.digitald4.biblical.store;

import com.google.inject.BindingAnnotation;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

public final class Annotations {
  private Annotations() {}

  @BindingAnnotation
  @Retention(RetentionPolicy.RUNTIME)
  public @interface ScriptureIndex {}

  @BindingAnnotation
  @Retention(RetentionPolicy.RUNTIME)
  public @interface CommandmentsIndex {}
}
