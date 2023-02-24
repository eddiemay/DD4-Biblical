package com.digitald4.biblical.server;

import com.digitald4.biblical.store.SearchIndexImpl;
import com.digitald4.biblical.util.*;
import com.digitald4.common.model.BasicUser;
import com.digitald4.common.server.APIConnector;
import com.digitald4.common.server.service.BasicUserService;
import com.digitald4.common.server.service.Echo;
import com.digitald4.common.storage.Annotations;
import com.digitald4.common.storage.DAO;
import com.digitald4.common.storage.GenericUserStore;
import com.digitald4.common.storage.SearchIndexer;
import com.digitald4.common.storage.UserStore;
import com.digitald4.common.util.ProviderThreadLocalImpl;
import com.google.common.collect.ImmutableList;
import com.google.inject.TypeLiteral;

import java.time.Duration;

public class EndPointsModule extends com.digitald4.common.server.EndPointsModule {

	public EndPointsModule() {
		super("dd4-biblical");
	}

	@Override
	public void configureServlets() {
		super.configureServlets();

		bind(Duration.class).annotatedWith(Annotations.SessionDuration.class)
				.toInstance(Duration.ofHours(8));
		bind(Boolean.class).annotatedWith(Annotations.SessionCacheEnabled.class).toInstance(false);

		ProviderThreadLocalImpl<BasicUser> userProvider = new ProviderThreadLocalImpl<>();
		bind(BasicUser.class).toProvider(userProvider);
		bind(new TypeLiteral<ProviderThreadLocalImpl<BasicUser>>(){}).toInstance(userProvider);
		bind(new TypeLiteral<UserStore<BasicUser>>(){})
				.toInstance(new GenericUserStore<>(BasicUser.class, getProvider(DAO.class)));

		bind(APIConnector.class).toInstance(new APIConnector(null, null, 100));

		bind(SearchIndexer.class).to(SearchIndexImpl.class);
		bind(ScriptureFetcher.class).to(ScriptureFetcherRouter.class);
		bind(ScriptureReferenceProcessor.class).to(ScriptureReferenceProcessorSplitImpl.class);
		bind(SunTimeUtil.class).to(SunTimeUtilSunriseSunsetOrg.class);

		configureEndpoints(
				getApiUrlPattern(),
				ImmutableList.of(
						Echo.class,
						BiblicalEventService.class,
						BookService.class,
						CalendarRuleService.class,
						CalendarValidatorService.class,
						CommandmentService.class,
						LessonService.class,
						ScriptureService.class,
						BasicUserService.class));
	}
}