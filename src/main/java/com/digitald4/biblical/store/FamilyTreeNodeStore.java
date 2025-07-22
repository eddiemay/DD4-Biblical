package com.digitald4.biblical.store;

import com.digitald4.biblical.model.BiblicalEvent;
import com.digitald4.biblical.model.FamilyTreeNode;
import com.digitald4.biblical.util.ScriptureMarkupProcessor;
import com.digitald4.common.storage.DAO;
import com.digitald4.common.storage.GenericStore;
import com.digitald4.common.storage.Transaction.Op;
import javax.inject.Inject;
import javax.inject.Provider;

public class FamilyTreeNodeStore extends GenericStore<FamilyTreeNode, Long> {
  private final Provider<DAO> daoProvider;
  private final ScriptureMarkupProcessor scriptureMarkupProcessor;
  @Inject
  public FamilyTreeNodeStore(
      Provider<DAO> daoProvider, ScriptureMarkupProcessor scriptureMarkupProcessor) {
    super(FamilyTreeNode.class, daoProvider);
    this.daoProvider = daoProvider;
    this.scriptureMarkupProcessor = scriptureMarkupProcessor;
  }

  @Override
  protected Op<FamilyTreeNode> preprocess(Op<FamilyTreeNode> op) {
    var entity = op.getEntity();
    entity.setSummary(scriptureMarkupProcessor.replaceScriptures(entity.getSummary()));
    var event = entity.getEventId() != null ?
        daoProvider.get().get(BiblicalEvent.class, entity.getEventId()) : null;
    entity.setBirthYear(event != null ? event.getYear() : null);
    entity.setDeathYear(event != null && event.getEndYear() > event.getYear() ? event.getEndYear() : null);
    return op;
  }
}
