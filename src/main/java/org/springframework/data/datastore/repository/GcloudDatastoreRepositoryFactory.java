package org.springframework.data.datastore.repository;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.Map;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.datastore.repository.query.GcloudDatastoreQueryCreator;
import org.springframework.data.datastore.repository.query.Query;
import org.springframework.data.projection.ProjectionFactory;
import org.springframework.data.repository.core.EntityInformation;
import org.springframework.data.repository.core.NamedQueries;
import org.springframework.data.repository.core.RepositoryInformation;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.repository.core.support.RepositoryFactorySupport;
import org.springframework.data.repository.query.EvaluationContextProvider;
import org.springframework.data.repository.query.ParametersParameterAccessor;
import org.springframework.data.repository.query.QueryLookupStrategy;
import org.springframework.data.repository.query.QueryLookupStrategy.Key;
import org.springframework.data.repository.query.QueryMethod;
import org.springframework.data.repository.query.RepositoryQuery;
import org.springframework.data.repository.query.ResultProcessor;
import org.springframework.data.repository.query.parser.PartTree;

import com.google.cloud.datastore.Datastore;
import com.google.cloud.datastore.DatastoreOptions;
import com.google.cloud.datastore.Entity;
import com.google.cloud.datastore.GqlQuery;
import com.google.cloud.datastore.QueryResults;

public class GcloudDatastoreRepositoryFactory
    extends RepositoryFactorySupport {
    private static final Logger log = LoggerFactory
            .getLogger(GcloudDatastoreRepositoryFactory.class);

    @Override
    public <T, ID extends Serializable> EntityInformation<T, ID> getEntityInformation(
        Class<T> domainClass) {
        return new GcloudDatastoreEntityInformation<T, ID>(domainClass);
    }

    @Override
    protected Class<?> getRepositoryBaseClass(RepositoryMetadata metadata) {
        return SimpleGcloudDatastoreRepository.class;
    }

    @Override
    protected Object getTargetRepository(RepositoryInformation information) {
        EntityInformation<?, Serializable> entityInformation =
            getEntityInformation(information.getDomainType());
        return getTargetRepositoryViaReflection(information, entityInformation);
    }

    @Override
    protected QueryLookupStrategy getQueryLookupStrategy(
            Key key,
            EvaluationContextProvider evaluationContextProvider) {
        return new QueryLookupStrategy() {
            @Override
            public RepositoryQuery resolveQuery(
                    Method method,
                    RepositoryMetadata metadata,
                    ProjectionFactory factory,
                    NamedQueries namedQueries) {
                QueryMethod queryMethod =
                    new QueryMethod(method, metadata, factory);
                ResultProcessor resultProcessor = queryMethod.getResultProcessor();
                Class<?> domainType = resultProcessor.getReturnedType().getDomainType();
                PartTree tree = new PartTree(method.getName(), domainType);
                return new RepositoryQuery() {
                    @Override
                    public Object execute(Object[] parameters) {
                        GcloudDatastoreQueryCreator queryCreator =
                            new GcloudDatastoreQueryCreator(
                                tree,
                                new ParametersParameterAccessor(
                                    queryMethod.getParameters(),
                                    parameters));
                        Query query = queryCreator.createQuery();
                         StringBuilder sb = new StringBuilder("SELECT * FROM Person ");
                        Map<String, Object> bindings = query.build(sb);
                        GqlQuery.Builder<Entity> queryBuilder =
                            com.google.cloud.datastore.Query.newGqlQueryBuilder(
                                com.google.cloud.datastore.Query.ResultType.ENTITY,
                                sb.toString());
                        for (String name : bindings.keySet()) {
                            Object value = bindings.get(name);
                            if (value instanceof CharSequence) {
                                queryBuilder.setBinding(name, ((CharSequence)value).toString());
                            }
                            else if (value instanceof Double || value instanceof Float) {
                                queryBuilder.setBinding(name, ((Number)value).doubleValue());
                            }
                            else if (value instanceof Number) {
                                queryBuilder.setBinding(name, ((Number)value).longValue());
                            }
                            else if (value instanceof Boolean) {
                                queryBuilder.setBinding(name, (Boolean)value);
                            }
                        }

                        Unmarshaller unmarshaller = new Unmarshaller();
                        DatastoreOptions datastoreOptions = DatastoreOptions.getDefaultInstance();
                        Datastore datastore = datastoreOptions.getService();
                        QueryResults<Entity> results = datastore.run(queryBuilder.build());
                        Iterable<Object> iterable = new Iterable<Object>() {
                            @Override
                            public Iterator<Object> iterator() {
                                return new Iterator<Object>() {
                                    @Override
                                    public boolean hasNext() {
                                        return results.hasNext();
                                    }

                                    @Override
                                    public Object next() {
                                        try {
                                            Object entity = domainType.newInstance();
                                            unmarshaller.unmarshalToObject(results.next(), entity);
                                            return entity;
                                        } catch (InstantiationException | IllegalAccessException e) {
                                            throw new IllegalStateException();
                                        }
                                    }
                                };
                            }
                        };

                        Stream<Object> result = StreamSupport.stream(iterable.spliterator(), false);
                        return resultProcessor.processResult(result);
                    }

                    @Override
                    public QueryMethod getQueryMethod() {
                        return queryMethod;
                    }
                };
            }
        };
    }

}
