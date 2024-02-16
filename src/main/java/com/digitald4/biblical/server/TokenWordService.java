package com.digitald4.biblical.server;

import com.digitald4.biblical.store.TokenWordStore;
import com.digitald4.biblical.util.HebrewConverter;
import com.digitald4.biblical.util.HebrewTokenizer.TokenWord;
import com.digitald4.common.exception.DD4StorageException;
import com.digitald4.common.exception.DD4StorageException.ErrorCode;
import com.digitald4.common.server.service.EntityServiceImpl;
import com.digitald4.common.storage.LoginResolver;
import com.google.api.server.spi.ServiceException;
import com.google.api.server.spi.config.Api;
import com.google.api.server.spi.config.ApiMethod;
import com.google.api.server.spi.config.ApiNamespace;
import com.google.api.server.spi.config.Named;
import com.google.api.server.spi.config.Nullable;
import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableList;
import javax.inject.Inject;

@Api(
    name = "tokenWords",
    version = "v1",
    namespace = @ApiNamespace(
        ownerDomain = "biblical.digitald4.com",
        ownerName = "biblical.digitald4.com"
    )
)
public class TokenWordService extends EntityServiceImpl<TokenWord, String> {
  private final TokenWordStore tokenWordStore;
  @Inject
  TokenWordService(TokenWordStore store, LoginResolver loginResolver) {
    super(store, loginResolver);
    this.tokenWordStore = store;
  }

  @ApiMethod(httpMethod = ApiMethod.HttpMethod.GET, path = "getTranslations")
  public ImmutableList<TokenWord> getTranslations(
      @Named("strongsId") @Nullable String strongsId) throws ServiceException {
    try {
      return tokenWordStore.getTranslations(HebrewConverter.toStrongsId(strongsId));
    } catch (DD4StorageException e) {
      throw new ServiceException(e.getErrorCode(), e);
    } catch (Exception e) {
      throw new ServiceException(ErrorCode.INTERNAL_SERVER_ERROR.getErrorCode(), e);
    }
  }

  @ApiMethod(httpMethod = ApiMethod.HttpMethod.GET, path = "all")
  public ImmutableCollection<TokenWord> getAll() throws ServiceException {
    try {
      return tokenWordStore.getAll();
    } catch (DD4StorageException e) {
      throw new ServiceException(e.getErrorCode(), e);
    } catch (Exception e) {
      throw new ServiceException(ErrorCode.INTERNAL_SERVER_ERROR.getErrorCode(), e);
    }
  }
}
