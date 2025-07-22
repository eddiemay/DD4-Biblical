package com.digitald4.biblical.server;

import com.digitald4.biblical.model.FamilyTreeNode;
import com.digitald4.biblical.store.FamilyTreeNodeStore;
import com.digitald4.common.exception.DD4StorageException;
import com.digitald4.common.server.service.EntityServiceBulkImpl;
import com.digitald4.common.storage.LoginResolver;
import com.google.api.server.spi.ServiceException;
import com.google.api.server.spi.config.Api;
import com.google.api.server.spi.config.ApiMethod;
import com.google.api.server.spi.config.ApiMethod.HttpMethod;
import com.google.api.server.spi.config.ApiNamespace;
import com.google.api.server.spi.config.Named;
import com.google.api.server.spi.config.Nullable;
import com.google.common.collect.ImmutableList;
import javax.inject.Inject;

@Api(
    name = "familyTreeNodes",
    version = "v1",
    namespace = @ApiNamespace(
        ownerDomain = "biblical.digitald4.com",
        ownerName = "biblical.digitald4.com"
    )
)
public class FamilyTreeNodeService extends EntityServiceBulkImpl<Long, FamilyTreeNode> {
  @Inject
  FamilyTreeNodeService(FamilyTreeNodeStore store, LoginResolver loginResolver) {
    super(store, loginResolver);
  }

  @ApiMethod(httpMethod = HttpMethod.POST, path = "batchCreate")
  public ImmutableList<FamilyTreeNode> batchCreate(
      FamilyTreeNodes nodes, @Nullable @Named("idToken") String idToken) throws ServiceException {
    try {
      resolveLogin(idToken, true);
      return getStore().create(nodes.getItems());
    } catch (DD4StorageException e) {
      throw new ServiceException(e.getErrorCode(), e);
    }
  }

  @Override
  protected boolean requiresLogin(String method) {
    return !"list".equals(method) && super.requiresLogin(method);
  }

  public static class FamilyTreeNodes {
    private ImmutableList<FamilyTreeNode> items = ImmutableList.of();

    public ImmutableList<FamilyTreeNode> getItems() {
      return items;
    }

    public FamilyTreeNodes setItems(Iterable<FamilyTreeNode> items) {
      this.items = ImmutableList.copyOf(items);
      return this;
    }
  }
}
