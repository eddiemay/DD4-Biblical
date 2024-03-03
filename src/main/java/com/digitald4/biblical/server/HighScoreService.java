package com.digitald4.biblical.server;

import com.digitald4.biblical.model.HighScore;
import com.digitald4.common.storage.Query;
import com.digitald4.common.storage.Query.Filter;
import com.digitald4.common.storage.Query.OrderBy;
import com.digitald4.common.storage.QueryResult;
import com.digitald4.common.storage.Store;
import com.google.api.server.spi.ServiceException;
import com.google.api.server.spi.config.Api;
import com.google.api.server.spi.config.ApiIssuer;
import com.google.api.server.spi.config.ApiMethod;
import com.google.api.server.spi.config.ApiNamespace;
import com.google.api.server.spi.config.DefaultValue;
import com.google.api.server.spi.config.Named;
import com.google.api.server.spi.config.Nullable;
import com.google.common.collect.ImmutableList;
import java.util.concurrent.atomic.AtomicInteger;
import javax.inject.Inject;
import javax.inject.Provider;
import javax.servlet.http.HttpServletRequest;

@Api(
    name = "highscores",
    version = "v1",
    namespace = @ApiNamespace(
        ownerDomain = "biblical.digitald4.com",
        ownerName = "biblical.digitald4.com"
    )
)
public class HighScoreService {
  private final Store<HighScore, Long> store;
  private final Provider<HttpServletRequest> requestProvider;

  @Inject
  HighScoreService(Store<HighScore, Long> store, Provider<HttpServletRequest> requestProvider) {
    this.store = store;
    this.requestProvider = requestProvider;
  }

  @ApiMethod(httpMethod = ApiMethod.HttpMethod.POST, path = "create")
  public AtomicInteger create(HighScore highScore) throws ServiceException {
    ImmutableList<HighScore> allScores = list(highScore.getGame(), highScore.getConfig(), 0, 0).getItems();
    store.create(highScore.setIpAddress(requestProvider.get().getRemoteAddr()));
    int rank = 0;
    for (; rank < allScores.size(); rank++) {
      HighScore compare = allScores.get(rank);
      if (highScore.getScore() > compare.getScore() || highScore.getScore() == compare.getScore()
          && highScore.getElapsedTime() < compare.getElapsedTime()) {
        break;
      }
    }

    return new AtomicInteger(rank + 1);
  }

  @ApiMethod(httpMethod = ApiMethod.HttpMethod.GET, path = "list")
  public QueryResult<HighScore> list(
      @Nullable @Named("game") String game, @Nullable @Named("config") String config,
      @Named("pageSize") @DefaultValue("0") int pageSize,
      @Named("pageToken") @DefaultValue("0") int pageToken) throws ServiceException {
    return store.list(
        Query.forList()
            .setFilters(Filter.of("game", game), Filter.of("config", config))
            .setOrderBys(
                OrderBy.of("score", true), OrderBy.of("elapsedTime"), OrderBy.of("startTime"))
            .setLimit(pageSize).setOffset(pageToken));
  }
}
