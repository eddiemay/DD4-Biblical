package com.digitald4.biblical.server;

import com.digitald4.biblical.model.LetterBox;
import com.digitald4.common.exception.DD4StorageException;
import com.digitald4.common.server.service.EntityServiceBulkImpl;
import com.digitald4.common.storage.LoginResolver;
import com.digitald4.common.storage.Store;
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
    name = "letterBoxs",
    version = "v1",
    namespace = @ApiNamespace(
        ownerDomain = "biblical.digitald4.com",
        ownerName = "biblical.digitald4.com"
    )
)
public class LetterBoxService extends EntityServiceBulkImpl<Long, LetterBox> {
  @Inject
  public LetterBoxService(Store<LetterBox, Long> store, LoginResolver loginResolver) {
    super(store, loginResolver);
  }

  @ApiMethod(httpMethod = HttpMethod.POST, path = "batchCreate")
  public ImmutableList<LetterBox> batchCreate(LetterBoxes letterBoxes, @Nullable @Named("idToken") String idToken)
      throws ServiceException {
    try {
      resolveLogin(idToken, "batchCreate");
      return getStore().create(letterBoxes.getItems());
    } catch (DD4StorageException e) {
      throw new ServiceException(e.getErrorCode(), e);
    }
  }

  @Override
  protected boolean requiresLogin(String method) {
    return !"list".equals(method) && super.requiresLogin(method);
  }

  public static class LetterBoxes {
    private ImmutableList<LetterBox> items = ImmutableList.of();

    public ImmutableList<LetterBox> getItems() {
      return items;
    }

    public LetterBoxes setItems(Iterable<LetterBox> items) {
      this.items = ImmutableList.copyOf(items);
      return this;
    }
  }
}
