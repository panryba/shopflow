shared-events/
└── src/main/java/com/example/shared/events/
    ├── PaymentRequestEvent.java
    ├── PaymentCompletedEvent.java
    ├── PaymentFailedEvent.java
    ├── RestaurantApprovedEvent.java
    ├── RestaurantRejectedEvent.java
    └── PaymentRollbackEvent.java

public record PaymentRequestEvent(
String eventId,
UUID orderId,
UUID customerId,
BigDecimal amount
) {

    public static PaymentRequestEvent of(UUID orderId, UUID customerId, BigDecimal amount) {
        return new PaymentRequestEvent(
            UUID.randomUUID().toString(),
            orderId,
			customerId,
			amount
        );
    }
}

public record PaymentCompletedEvent(
String eventId,
UUID orderId
) {

    public static PaymentCompletedEvent of(UUID orderId) {
        return new PaymentCompletedEvent(
            UUID.randomUUID().toString(),
            orderId
        );
    }
}

public record PaymentFailedEvent(
String eventId,
UUID orderId,
String reason
) {

    public static PaymentFailedEvent of(UUID orderId, String reason) {
        return new PaymentFailedEvent(
            UUID.randomUUID().toString(),
            orderId,
			reason
        );
    }
}

public record RestaurantRequestEvent(
String eventId,
UUID orderId
) {

    public static RestaurantRequestEvent of(UUID orderId) {
        return new RestaurantRequestEvent(
            UUID.randomUUID().toString(),
            orderId
        );
    }
}

public record RestaurantApprovedEvent(
String eventId,
UUID orderId
) {

    public static RestaurantApprovedEvent of(UUID orderId) {
        return new RestaurantApprovedEvent(
            UUID.randomUUID().toString(),
            orderId
        );
    }
}

public record RestaurantRejectedEvent(
String eventId,
UUID orderId,
String reason
) {

    public static RestaurantRejectedEvent of(UUID orderId, String reason) {
        return new RestaurantRejectedEvent(
            UUID.randomUUID().toString(),
            orderId,
			reason
        );
    }
}

public record PaymentRollbackEvent(
String eventId,
UUID orderId
) {

    public static PaymentRollbackEvent of(UUID orderId) {
        return new PaymentRollbackEvent(
            UUID.randomUUID().toString(),
            orderId
        );
    }
}




order-service/
└── src/main/java/com/example/order/
    ├── domain/                                   <-- pure business logic
    │   ├── model/
    │   │	├── Order.java
    │   │	├── OrderItem.java
    │   │	└── OrderStatus.java
    │   ├── event/
    │   ├── valueobject/
    │   │	├── Money.java
    │   │	└── OrderId.java
    │	└── service/
    │
    ├── application/                              <-- orchestration (use cases)
    │	├── port/
    │   │   ├── input/
    │	│	│	└── OrderUseCase.java
    │   │   └── output/
    │	│		├── OrderRepository.java
    │   │       └── OrderEventPublisher.java
    │   ├── service/
    │   │	└── OrderApplicationService.java
    │   └── saga/
    │   	└── OrderSagaOrchestrator.java
    │
    ├── infrastructure/                            <-- DB, Kafka
    │   ├── persistence/
    │   │	├── entity/
    │   │   │   ├── OrderEntity.java
    │   │   │   └── OrderItemEntity.java
    │   │	├── mapper/
    │   │   │   └── OrderMapper.java
    │   │	├── repository/
    │   │   │   └── PanacheOrderRepository.java
    │   │	└── idempotency/
    │   │       ├── ProcessedEventEntity.java
    │   │       ├── ProcessedEventRepository.java
    │   │       └── ProcessedEventCleanupJob.java
    │   │
    │   ├── messaging/
    │   │	├── KafkaOrderEventPublisher.java
    │   │	└── OrderEventConsumer.java
    │	│
    │	├── observability/
    │   │	├── CorrelationIdProvider.java
    │	│	└── CorrelationIdFilter.java
    │   │
    │	└── outbox/                  
    │	    ├── OutboxEventEntity.java
    │	    ├── OutboxRepository.java
    │	    ├── OutboxService.java
    │	    ├── OutboxPublisherJob.java
    │	    ├── OutboxEventType.java
    │	    └── OutboxHealthCheck.java
    │
    └── presentation/                               <-- REST     (naming options: api/, presentation/, web/, rest/)
        ├── OrderResource.java
        ├── dto/
        │	├── CreateOrderRequest.java
        │	├── OrderItemRequest.java
        │	├── OrderResponse.java
        │   └── OrderItemResponse.java
        ├── mapper/
        │	└── OrderPresentationMapper.java
        └── exception/
            ├── ErrorResponse.java
            └── mapper/
                ├── NotFoundExceptionMapper.java
                ├── ConstraintViolationExceptionMapper.java
                ├── OptimisticLockExceptionMapper.java
                └── GenericExceptionMapper.java


@Path("/orders")
@Consumes("application/json")
@Produces("application/json")
public class OrderResource {

	@Inject
    OrderUseCase service;

    @Inject
    OrderSagaOrchestrator orchestrator;
	
	@Inject
    OrderPresentationMapper mapper;

    @POST
	@Retry(
		maxRetries = 3, 
		delay = 500, 
		jitter = 200, 
		retryOn = OptimisticLockException.class
	)
	public Response create(@Valid CreateOrderRequest request) {
		UUID orderId = orchestrator.start(request);
		return Response.accepted()                                  //accepted() not created() since order creation is async (when return is called Order hasnt been actually completed)
			.header("Location", "/orders/" + orderId)
			.build();
	}
	
	@PUT
    @Path("/{id}/cancel")
    @Retry(maxRetries = 3, delay = 200, jitter = 200,
           retryOn = OptimisticLockException.class,
           abortOn = NotFoundException.class)
    public Response cancel(@PathParam("id") UUID id) {
        service.cancel(new OrderId(id));
        return Response.noContent().build();
    }
	
	

	
	@GET
    @Path("/{id}")
	@Retry(
		maxRetries = 3,
		delay = 200,
		jitter = 200,
		retryOn = PersistenceException.class,
		abortOn = NotFoundException.class   
	)
    public OrderResponse getById(@PathParam("id") UUID id) {
        Order order = service.findById(new OrderId(id));
        return mapper.toResponse(order);
    }
}


📤 DTO

public record CreateOrderRequest(

    @NotNull
    UUID customerId,

    @NotNull
    @DecimalMin("0.01")
    BigDecimal amount,

    @NotEmpty
    @Valid
    List<OrderItemRequest> items
) {
public record OrderItemRequest(

        @NotNull
        UUID productId,

        @Min(1)
        int quantity,

        @NotNull
        @DecimalMin("0.01")
        BigDecimal price
    ) {}
}

public record OrderResponse(
UUID id,
OrderStatus status,
List<OrderItemResponse> items,
BigDecimal total
) {
public record OrderItemResponse(
UUID productId,
int quantity,
BigDecimal price
) {}
}

📤 DTO Mapper

ApplicationScoped
public class OrderPresentationMapper {

    public OrderResponse toResponse(Order order) {
        List<OrderResponse.OrderItemResponse> items = order.getItems().stream()
            .map(i -> new OrderResponse.OrderItemResponse(
                i.productId(),
                i.quantity(),
                i.price().amount()
            ))
            .toList();

        BigDecimal total = order.getItems().stream()
            .map(i -> i.price().amount().multiply(BigDecimal.valueOf(i.quantity())))
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new OrderResponse(
            order.getId().value(),
            order.getStatus(),
            items,
            total
        );
    }
}

OR:
<dependency>
<groupId>org.mapstruct</groupId>
<artifactId>mapstruct</artifactId>
<version>1.5.5.Final</version>
</dependency>
<dependency>
<groupId>org.mapstruct</groupId>
<artifactId>mapstruct-processor</artifactId>
<version>1.5.5.Final</version>
<scope>provided</scope>
</dependency>
@Mapper(componentModel = MappingConstants.ComponentModel.CDI)
public interface OrderPresentationMapper {

    @Mapping(target = "id", expression = "java(order.getId().value())")
    @Mapping(target = "items", expression = "java(mapItems(order.getItems()))")
    @Mapping(target = "total", expression = "java(calculateTotal(order))")
    OrderResponse toResponse(Order order);

    default List<OrderResponse.OrderItemResponse> mapItems(List<OrderItem> items) {
        return items.stream()
            .map(i -> new OrderResponse.OrderItemResponse(
                i.productId(),
                i.quantity(),
                i.price().amount()
            ))
            .toList();
    }

    default BigDecimal calculateTotal(Order order) {
        return order.getItems().stream()
            .map(i -> i.price().amount().multiply(BigDecimal.valueOf(i.quantity())))
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}


📤 Exception Mapper

public class CorrelationIdProvider {
public static String get() {
return UUID.randomUUID().toString();
}
}

public record ErrorResponse(
int status,
String code,
String message,
LocalDateTime timestamp,
String correlationId,
List<String> errors
) {
public static ErrorResponse of(int status, String code, String message, String correlationId) {
return new ErrorResponse(
status,
code,
message,
LocalDateTime.now(),
correlationId,
null
);
}

    public static ErrorResponse of(int status, String code, String message, String correlationId, List<String> errors) {
        return new ErrorResponse(
            status,
            code,
            message,
            LocalDateTime.now(),
            correlationId,
            errors
        );
    }
}

@Provider
public class OptimisticLockExceptionMapper
implements ExceptionMapper<OptimisticLockException> {

    @Override
    public Response toResponse(OptimisticLockException exception) {

        String correlationId = CorrelationIdProvider.get();
        Response.Status status = Response.Status.CONFLICT;

        return Response.status(status)
            .entity(ErrorResponse.of(
                status.getStatusCode(),
                "CONCURRENT_MODIFICATION",
                "Resource was modified concurrently. Please retry.",
                correlationId
            ))
            .build();
    }
}

@Provider
public class NotFoundExceptionMapper
implements ExceptionMapper<NotFoundException> {

    @Override
    public Response toResponse(NotFoundException exception) {

        String correlationId = CorrelationIdProvider.get();
        Response.Status status = Response.Status.NOT_FOUND;

        String message = exception.getMessage() != null
            ? exception.getMessage()
            : "Resource not found";

        return Response.status(status)
            .entity(ErrorResponse.of(
                status.getStatusCode(),
                "NOT_FOUND",
                message,
                correlationId
            ))
            .build();
    }
}

@Provider
public class ConstraintViolationExceptionMapper
implements ExceptionMapper<ConstraintViolationException> {

    @Override
    public Response toResponse(ConstraintViolationException exception) {

        String correlationId = CorrelationIdProvider.get();
        Response.Status status = Response.Status.BAD_REQUEST;

        List<String> errors = exception.getConstraintViolations()
            .stream()
            .map(v -> {
                String field = v.getPropertyPath().toString();
                field = field.substring(field.lastIndexOf('.') + 1);
                return field + ": " + v.getMessage();
            })
            .toList();

        return Response.status(status)
            .entity(ErrorResponse.of(
                status.getStatusCode(),
                "VALIDATION_ERROR",
                "Validation failed",
                correlationId,
                errors
            ))
            .build();
    }
}

import io.quarkus.logging.Log;
@Provider
public class GenericExceptionMapper
implements ExceptionMapper<Exception> {

    @Override
    public Response toResponse(Exception exception) {

        String correlationId = CorrelationIdProvider.get();
        Response.Status status = Response.Status.INTERNAL_SERVER_ERROR;

        Log.errorf(exception, "Unhandled exception [correlationId=%s]", correlationId);

        return Response.status(status)
            .entity(ErrorResponse.of(
                status.getStatusCode(),
                "INTERNAL_ERROR",
                "Unexpected error occurred",
                correlationId
            ))
            .build();
    }
}

📤 dto
CreateOrderRequest



🚀 2.4 APPLICATION

Service:

@ApplicationScoped
public class OrderApplicationService implements OrderUseCase {

    @Inject
    OrderRepository repository;

    public void create(Order order) {
        order.confirm();
        repository.save(order);
    }
	
	public void pay(OrderId orderId) {
        Order order = repository.findById(orderId).orElseThrow();
        order.pay();
        repository.update(order);
    }

    public void approve(OrderId orderId) {
        Order order = repository.findById(orderId).orElseThrow();
        order.approve();
        repository.update(order);
    }

    public void cancel(OrderId orderId) {
        Order order = repository.findById(orderId).orElseThrow();
        order.cancel();
        repository.update(order);
    }
	
	public Order findById(OrderId id) {
        return repository.findById(id)
            .orElseThrow(() -> new NotFoundException("Order not found: " + id.value()));
    }
}


Orchestrator:

@ApplicationScoped
public class OrderSagaOrchestrator {

    @Inject
    OutboxService outbox;

    @Inject
    OrderUseCase service;

    // START SAGA
	@Transactional
    public UUID start(CreateOrderRequest request) {
		Order order = new Order(new OrderId(UUID.randomUUID()));
		request.items().forEach(i -> order.addItem(i.productId(), i.quantity(), i.price()));
        service.create(order);
		outbox.save(
			Order.class.getSimpleName(),
            order.getId().value().toString(),
            OutboxEventType.PAYMENT_REQUEST,
            PaymentRequestEvent.of(order.getId().value(), request.customerId(), request.amount())
        );
		return order.getId().value();
    }
}

    // STEP 1 SUCCESS
	@Transactional
    public void onPaymentCompleted(PaymentCompletedEvent event) {
		if (!tryProcess(event.eventId())) return;
		service.pay(new OrderId(event.orderId()));
		outbox.save(
			Order.class.getSimpleName(),
            event.orderId().toString(),
            OutboxEventType.RESTAURANT_REQUEST,
            RestaurantRequestEvent.of(event.orderId())
        );
    }	

    // STEP 1 FAIL
	@Transactional
    public void onPaymentFailed(PaymentFailedEvent event) {
		if (!tryProcess(event.eventId())) return;
        service.cancel(new OrderId(event.orderId()));
    }

    // STEP 2 SUCCESS
	@Transactional
    public void onRestaurantApproved(RestaurantApprovedEvent event) {
		if (!tryProcess(event.eventId())) return;
        service.approve(new OrderId(event.orderId()));
    }

    // STEP 2 FAIL → COMPENSATION
	@Transactional
    public void onRestaurantRejected(RestaurantRejectedEvent event) {
		if (!tryProcess(event.eventId())) return;
		service.cancel(new OrderId(event.orderId()));
		outbox.save(
			Order.class.getSimpleName(),
            event.orderId().toString(),
            OutboxEventType.PAYMENT_ROLLBACK,
            PaymentRollbackEvent.of(event.orderId())
        );
    }
	
	// IDEMPOTENCY
    private boolean tryProcess(String eventId) {
        try {
            processedRepository.save(eventId);
            return true;
        } catch (ConstraintViolationException e) {
			// unique constraint violation → duplicate
            return false;
        }
    }
}



🧠 2.5 DOMAIN -> business logic - no @Entity

@Getter
public class Order {

    private OrderId id;
    private List<OrderItem> items = new ArrayList<>();
    private OrderStatus status;
	
	
	// for NEW orders
    public Order(OrderId id) {
        this.id = id;
        this.status = OrderStatus.PENDING;
    }

    // for DB reconstruction ONLY
    public static Order reconstitute(
            OrderId id,
            List<OrderItem> items,
            OrderStatus status
    ) {
        Order order = new Order(id);
        order.items = new ArrayList<>(items);
        order.status = status;
        return order;
    }

    public void addItem(UUID productId, int quantity, BigDecimal price) {
        if (quantity <= 0) throw new IllegalArgumentException();

        items.add(new OrderItem(
                UUID.randomUUID(),
                this.id,
                productId,
                quantity,
                new Money(price)
        ));
    }

    public void confirm() {
        if (items.isEmpty()) throw new IllegalStateException();
        this.status = OrderStatus.CREATED;
    }

    public void pay() {
        if (status != OrderStatus.CREATED) {
            throw new IllegalStateException("Invalid state transition");
        }
        this.status = OrderStatus.PAID;
    }

    public void approve() {
        if (status != OrderStatus.PAID) {
            throw new IllegalStateException("Cannot approve");
        }
        this.status = OrderStatus.APPROVED;
    }

    public void cancel() {
        this.status = OrderStatus.CANCELLED;
    }
}

public record OrderItem(
UUID id;
OrderId orderId;
UUID productId;
int quantity;
Money price;
) {}

public enum OrderStatus {
PENDING, CREATED, PAID, APPROVED, CANCELLED
}

public record Money(BigDecimal amount) {

	public Money {
		Objects.requireNonNull(amount, "Amount cannot be null");
		if (amount.compareTo(BigDecimal.ZERO) < 0) 
			throw new IllegalArgumentException("Amount cannot be negative");
	}

    public static final Money ZERO = new Money(BigDecimal.ZERO);

    public boolean isGreaterThanZero() {
        return this.amount != null && this.amount.compareTo(BigDecimal.ZERO) > 0;
    }
	
    public Money add(Money money) {
        return new Money(setScale(this.amount.add(money.getAmount())));
    }
}

public record OrderId(UUID value) {}



🔌 2.6 PORTS → interfaces (decoupling layer)

public interface OrderUseCase {
void create(Order order);
void pay(OrderId orderId);
void approve(OrderId orderId)
void cancel(OrderId orderId);
Order findById(OrderId id);
}

public interface OrderRepository {
void save(Order order);
Optional<Order> findById(OrderId id);
void update(Order order);
}

public interface OrderEventPublisher {
void publishPaymentRequest(PaymentRequestEvent event);
void publishRestaurantRequest(RestaurantRequestEvent event)
void publishPaymentRollback(PaymentRollbackEvent event);
}



🔴 2.7 INFRASTRUCTURE → implementations

📤 2.7.1 persistence:
@ApplicationScoped
public class PanacheOrderRepository
implements OrderRepository, PanacheRepositoryBase<OrderEntity, UUID> {

    @Inject
    OrderMapper mapper;

    @Override
    public Optional<Order> findById(OrderId id) {
        return findByIdOptional(id.value())
                .map(mapper::toDomain);
    }
	
	@Override
	public void save(Order order) {
		persist(mapper.toEntity(order, new OrderEntity()));
	}

	@Override
	public void update(Order order) {
		OrderEntity managed = findByIdOptional(order.getId().value())
				.orElseThrow(() -> new NotFoundException("Order not found: " + order.getId().value()));

		mapper.toEntity(order, managed);
		// no persist/merge needed — managed entity auto-tracked by Hibernate
	}
}



@Entity
@Table(name = "orders")
@Getter
@Setter
@NoArgsConstructor
public class OrderEntity {

    @Id
    private UUID id;
	
	@Version
    private long version;

    @Enumerated(EnumType.STRING)
    private OrderStatus status;

    @OneToMany(
            mappedBy = "order",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    private List<OrderItemEntity> items = new ArrayList<>();
}

@Entity
@Table(name = "order_items")
@Getter
@Setter
@NoArgsConstructor
public class OrderItemEntity {

    @Id
    private UUID id;

    private UUID productId;

    private int quantity;

    private BigDecimal price;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    private OrderEntity order;
}

@ApplicationScoped
public class OrderMapper {

    // CREATE — pass new OrderEntity()
    // UPDATE — pass existing managed entity
    public OrderEntity toEntity(Order order, OrderEntity entity) {
        entity.setId(order.getId().value());
        entity.setStatus(order.getStatus());

        Map<UUID, OrderItemEntity> existing = entity.getItems().stream()
                .collect(Collectors.toMap(OrderItemEntity::getId, item -> item));

        List<OrderItemEntity> items = order.getItems().stream()
                .map(item -> {
                    OrderItemEntity itemEntity = existing.getOrDefault(
                            item.id(),
                            new OrderItemEntity()
                    );
                    itemEntity.setId(item.id());
                    itemEntity.setProductId(item.productId());
                    itemEntity.setQuantity(item.quantity());
                    itemEntity.setPrice(item.price().amount());
                    itemEntity.setOrder(entity);
                    return itemEntity;
                })
                .toList();

        entity.getItems().clear();
        entity.getItems().addAll(items);
        return entity;
    }

    public Order toDomain(OrderEntity entity) {
        List<OrderItem> items = entity.getItems().stream()
                .map(e -> new OrderItem(
                        e.getId(),
                        new OrderId(entity.getId()),
                        e.getProductId(),
                        e.getQuantity(),
                        new Money(e.getPrice())
                ))
                .toList();

        return Order.reconstitute(
                new OrderId(entity.getId()),
                items,
                entity.getStatus()
        );
    }
}
📤 2.7.2 Idempotency
@Entity
@Table(name = "order_processed_events")
@Getter
@Setter
@NoArgsConstructor
public class ProcessedEventEntity {

    @Id
    private String eventId;

    @Column(nullable = false, updatable = false)
    private Instant processedAt;

    @PrePersist
    void prePersist() {
        this.processedAt = Instant.now();
    }
}

@ApplicationScoped
public class ProcessedEventRepository
implements PanacheRepository<ProcessedEventEntity> {

	@Transactional
    public void save(String eventId) {
        ProcessedEventEntity entity = new ProcessedEventEntity();
        entity.setEventId(eventId);
        persist(entity);
    }

    @Transactional
    public long deleteOlderThan(Instant cutoff) {
        return delete("processedAt < ?1", cutoff);
    }
}

pom:
<dependency>
<groupId>io.quarkus</groupId>
<artifactId>quarkus-scheduler</artifactId>
</dependency>

application.properties:
app.idempotency.retention-days=7

import io.quarkus.logging.Log;
@ApplicationScoped
public class ProcessedEventCleanupJob {

	@Inject
    ProcessedEventRepository repository;

    @ConfigProperty(name = "app.idempotency.retention-days")
    int retentionDays;

    @Scheduled(every = "1h")
    void cleanup() {

        Instant cutoff = Instant.now().minus(retentionDays, ChronoUnit.DAYS);

        long deleted = repository.deleteOlderThan(cutoff);

        Log.infov("Cleanup removed: {0,number,#}", deleted);
    }
}

📤 2.7.3 Outbox

@Entity
@Table(name = "outbox_events",
indexes = {
@Index(columnList = "processed, retry_count"),
@Index(columnList = "processed, retry_count, created_at)")
})
@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class OutboxEventEntity {

    @Id
    private String id;

	@Column(name = "AGGREGATE_ID")
    private String aggregateId;

    private String aggregateType;

    @Enumerated(EnumType.STRING)
	@Column(name = "EVENT_TYPE")
    private OutboxEventType eventType;

    @Lob
    private String payload;

	@Column(name = "CREATED_AT", nullable = false, updatable = false)
    private Instant createdAt;
	
	@Column(name = "PROCESSED_AT")
	private Instant processedAt;
	
	private boolean processed;
	
	@Column(name = "RETRY_COUNT")
	private int retryCount;

    @Lob
	@Column(name = "LAST_ERROR")
    private String lastError;

    @PrePersist
    void setCreationDate() {
        this.createdAt = Instant.now();
    }
}

public enum OutboxEventType {
PAYMENT_REQUEST,
RESTAURANT_REQUEST,
PAYMENT_ROLLBACK
}

@ApplicationScoped
public class OutboxRepository implements PanacheRepository<OutboxEventEntity> {

    @Inject
    EntityManager em;

    public List<OutboxEventEntity> findUnprocessed(int limit, int maxRetries) {
		if (limit <= 0) throw new IllegalArgumentException("limit must be > 0");
		
        return em.createNativeQuery("""
            SELECT * FROM outbox_events
            WHERE processed = false
              AND retry_count < :maxRetries
            ORDER BY created_at
            FOR UPDATE SKIP LOCKED
            LIMIT :limit
        """, OutboxEventEntity.class)
        .setParameter("limit", limit)
        .setParameter("maxRetries", maxRetries)
        .getResultList();
    }
	
	public List<OutboxEventEntity> findDead(int maxRetries) {
		return find("retryCount >= ?1 and processed = false", maxRetries).list();
	}
	
	public long countDead(int maxRetries) {
		return count("retryCount >= ?1 and processed = false", maxRetries);
	}
	
	public void deleteProcessed(Instant cutoff) {
		delete("processed = true and processedAt < ?1", cutoff);
	}
}

@ApplicationScoped
public class OutboxService {

    @Inject
    OutboxRepository repository;

    @Inject
    ObjectMapper objectMapper;

    public void save(String aggregateType, String aggregateId, OutboxEventType eventType, Object event) {
        try {
			OutboxEventEntity entity = OutboxEventEntity.builder()
				.id(UUID.randomUUID().toString())
				.aggregateId(aggregateId)
				.aggregateType(aggregateType)
				.eventType(eventType)
				.payload(objectMapper.writeValueAsString(event))
				.processed(false)
				.build();
			repository.persist(entity);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}

pom:
<dependency>
<groupId>io.quarkus</groupId>
<artifactId>quarkus-scheduler</artifactId>
</dependency>

application.properties:
app.outbox.batch-size=50
app.outbox.max-retries=5
app.outbox.retention-days=7

//Sends Events to Kafka (publish Events)
@ApplicationScoped
public class OutboxPublisherJob {

    @Inject
    OutboxRepository repository;

    @Inject
    KafkaOrderEventPublisher kafka;

    @Inject
    ObjectMapper objectMapper;
	
	@ConfigProperty(name = "app.outbox.batch-size")
    int batchSize;

    @ConfigProperty(name = "app.outbox.max-retries")
    int maxRetries;
	
	@ConfigProperty(name = "app.outbox.retention-days")
	int retentionDays;
	
	@Scheduled(every = "5s")
    public void publish() {
        repository.findUnprocessed(batchSize, maxRetries).forEach(event -> processPublish(event));
    }
	
	@Transactional
	void processPublish(OutboxEventEntity event) {
		try {
			route(event);
			event.setProcessed(true);
			event.setProcessedAt(Instant.now());
		} catch (Exception e) {
			event.setRetryCount(event.getRetryCount() + 1);
			event.setLastError(e.getMessage());
			// DO NOT rethrow → continue processing others
		}
	}

    private void route(OutboxEventEntity event) throws Exception {

        switch (event.getEventType()) {

            case OutboxEventType.PAYMENT_REQUEST -> {
                var payload = objectMapper.readValue(
                        event.getPayload(),
                        com.example.shared.events.PaymentRequestEvent.class
                );
                kafka.publishPaymentRequest(payload);
            }

            case OutboxEventType.RESTAURANT_REQUEST -> {
                var payload = objectMapper.readValue(
                        event.getPayload(),
                        com.example.shared.events.RestaurantRequestEvent.class
                );
                kafka.publishRestaurantRequest(payload);
            }

            case OutboxEventType.PAYMENT_ROLLBACK -> {
                var payload = objectMapper.readValue(
                        event.getPayload(),
                        com.example.shared.events.PaymentRollbackEvent.class
                );
                kafka.publishPaymentRollback(payload);
            }
        }
    }
	
	//Logs dead Events from Outbox table
	@Scheduled(every = "1m")
	@Transactional
	public void logDeadEvents() {
		var dead = repository.findDead(maxRetries);
		dead.forEach(e ->
			Log.error("DEAD OUTBOX EVENT {} error={}", e.getId(), e.getLastError())
		);
	}


	//deletes processed Events from Outbox table
	@Scheduled(every = "1h")
	@Transactional
	public void cleanup() {		
		Instant cutoff = Instant.now().minus(retentionDays, ChronoUnit.DAYS);
		repository.deleteProcessed(cutoff);
	}
}



@Readiness
@ApplicationScoped
public class OutboxHealthCheck implements HealthCheck {

    @Inject
    OutboxRepository repository;

    @ConfigProperty(name = "app.outbox.max-retries")
    int maxRetries;

    @Override
    public HealthCheckResponse call() {
        long deadCount = repository.countDead(maxRetries);
		return HealthCheckResponse.named("outbox")
				.status(deadCount == 0)                           //status(true) means health status UP, status(false) means health status DOWN - so status(true) = UP when no dead events: deadCount == 0
				.withData("deadEvents", deadCount)
				.build();
	}
}


📤 2.7.4 Messaging

@ApplicationScoped
public class KafkaOrderEventPublisher implements OrderEventPublisher {

    @Channel("payment-request")
    Emitter<PaymentRequestEvent> paymentEmitter;

    @Channel("restaurant-request")
    Emitter<RestaurantRequestEvent> restaurantEmitter;

    @Channel("payment-rollback")
    Emitter<PaymentRollbackEvent> rollbackEmitter;

    public void publishPaymentRequest(PaymentRequestEvent event) {
        sendWithKey(paymentEmitter, event.orderId().toString(), event);
    }

    public void publishRestaurantRequest(RestaurantRequestEvent event) {
        sendWithKey(restaurantEmitter, event.orderId().toString(), event);
    }

    public void publishPaymentRollback(PaymentRollbackEvent event) {
        sendWithKey(rollbackEmitter, event.orderId().toString(), event);
    }

    private <T> void sendWithKey(Emitter<T> emitter, String key, T payload) {
        var metadata = OutgoingKafkaRecordMetadata.builder()
			.withKey(key)
			.build();
        emitter.send(Message.of(payload).addMetadata(metadata));
    }
}

@ApplicationScoped
public class OrderEventConsumer {

    @Inject
    OrderSagaOrchestrator orchestrator;

    @Inject
    ProcessedEventRepository processedRepository;

    @Incoming("payment-completed")
    @Retry(maxRetries = 3, delay = 500, jitter = 200, retryOn = OptimisticLockException.class)
	public void onPaymentCompleted(PaymentCompletedEvent event) {
        orchestrator.onPaymentCompleted(event);
    }

    @Incoming("payment-failed")
    @Retry(maxRetries = 3, delay = 500, jitter = 200, retryOn = OptimisticLockException.class)
    public void onPaymentFailed(PaymentFailedEvent event) {
        orchestrator.onPaymentFailed(event);
    }

    @Incoming("restaurant-approved")
    @Retry(maxRetries = 3, delay = 500, jitter = 200, retryOn = OptimisticLockException.class)
    public void onRestaurantApproved(RestaurantApprovedEvent event) {
        orchestrator.onRestaurantApproved(event);
    }

    @Incoming("restaurant-rejected")
	@Retry(maxRetries = 3, delay = 500, jitter = 200, retryOn = OptimisticLockException.class)
    public void onRestaurantRejected(RestaurantRejectedEvent event) {
        orchestrator.onRestaurantRejected(event);
    }
}



payment-service

Reacts to:
payment-request
payment-rollback

Emits:
payment-completed
payment-failed

payment-service/
└── src/main/java/com/example/payment/
├── domain/
│   └── model/      
│
├── application/
│   └── (optional for now)
│
├── infrastructure/       
│   └── messaging/
│   	└── PaymentEventConsumer.java
│
└── presentation/
└── (optional for now)

✅ 3.1 Dependency (same)
<dependency>
<groupId>io.quarkus</groupId>
<artifactId>quarkus-smallrye-reactive-messaging-kafka</artifactId>
</dependency>

⚙️ 3.2 application.properties
# Producer → send result
mp.messaging.outgoing.payment-completed.connector=smallrye-kafka
mp.messaging.outgoing.payment-completed.topic=payment-completed
mp.messaging.outgoing.payment-completed.value.serializer=io.quarkus.kafka.client.serialization.ObjectMapperSerializer
mp.messaging.outgoing.payment-failed.connector=smallrye-kafka
mp.messaging.outgoing.payment-failed.topic=payment-failed
mp.messaging.outgoing.payment-failed.serializer=io.quarkus.kafka.client.serialization.ObjectMapperSerializer
# Consumer → receive order
mp.messaging.incoming.payment-request.connector=smallrye-kafka
mp.messaging.incoming.payment-request.topic=payment-request
mp.messaging.incoming.payment-request.group.id=payment-service
mp.messaging.incoming.payment-request.failure-strategy=dead-letter-queue
mp.messaging.incoming.payment-request.dead-letter-queue.topic=payment-request-dlq
mp.messaging.incoming.payment-request.max-retries=5
mp.messaging.incoming.payment-request.retry-backoff.initial=1s
mp.messaging.incoming.payment-request.retry-backoff.max=30s
mp.messaging.incoming.payment-request.retry-backoff.factor=2.0
mp.messaging.incoming.payment-request.value.deserializer=io.quarkus.kafka.client.serialization.ObjectMapperDeserializer
mp.messaging.incoming.payment-request.value.deserializer.value-type=com.example.shared.events.PaymentRequestEvent
mp.messaging.incoming.payment-rollback.connector=smallrye-kafka
mp.messaging.incoming.payment-rollback.topic=payment-rollback
mp.messaging.incoming.payment-rollback.group.id=payment-service
mp.messaging.incoming.payment-rollback.failure-strategy=dead-letter-queue
mp.messaging.incoming.payment-rollback.dead-letter-queue.topic=payment-rollback-dlq
mp.messaging.incoming.payment-rollback.max-retries=5
mp.messaging.incoming.payment-rollback.retry-backoff.initial=1s
mp.messaging.incoming.payment-rollback.retry-backoff.max=30s
mp.messaging.incoming.payment-rollback.retry-backoff.factor=2.0
mp.messaging.incoming.payment-rollback.value.deserializer=io.quarkus.kafka.client.serialization.ObjectMapperDeserializer
mp.messaging.incoming.payment-rollback.value.deserializer.value-type=com.example.shared.events.PaymentRollbackEvent

📥 3.3 Consumer + Producer
import io.quarkus.logging.Log;
@ApplicationScoped
public class PaymentEventConsumer {

    @Channel("payment-completed")
    Emitter<PaymentCompletedEvent> successEmitter;

    @Channel("payment-failed")
    Emitter<PaymentFailedEvent> failedEmitter;
	
    @Incoming("payment-request")
    public void process(PaymentRequestEvent event) {

        boolean success = processPayment(event);

        if (success) {
            Log.info("PAYMENT SUCCESS");

            successEmitter.send(
                Message.of(new PaymentCompletedEvent.of(event.orderId()))
                    .addMetadata(key(event.orderId()))
            );
        } else {
            Log.info("PAYMENT FAILED");

            failedEmitter.send(
                Message.of(new PaymentFailedEvent.of(event.orderId(), "Insufficient funds"))
					.addMetadata(key(event.orderId()))
            );
        }
    }
	
	@Incoming("payment-rollback")
	public void rollback(PaymentRollbackEvent event) {
		Log.infov("PAYMENT ROLLBACK id={0,number,#}", event.orderId());
	}
	
	private boolean processPayment(PaymentRequestEvent event) {
        return event.amount().doubleValue() < 1000;
    }

    private OutgoingKafkaRecordMetadata<UUID> key(UUID orderId) {
        return OutgoingKafkaRecordMetadata.<UUID>builder()
                .withKey(orderId)
                .build();
    }
}





restaurant-service

Reacts to:
restaurant-request

Emits:
restaurant-approved
restaurant-rejected

restaurant-service/
└── src/main/java/com/example/restaurant/
├── domain/
│   └── model/      
│
├── application/
│   └── (optional for now)
│
├── infrastructure/       
│   └── messaging/
│   	└── RestaurantEventConsumer.java
│
└── presentation/
└── (optional for now)

✅ 4.1 Dependency (same)
<dependency>
<groupId>io.quarkus</groupId>
<artifactId>quarkus-smallrye-reactive-messaging-kafka</artifactId>
</dependency>

⚙️ 4.2 application.properties
# Producer → send result
mp.messaging.outgoing.restaurant-approved.connector=smallrye-kafka
mp.messaging.outgoing.restaurant-approved.topic=restaurant-approved
mp.messaging.outgoing.restaurant-approved.value.serializer=io.quarkus.kafka.client.serialization.ObjectMapperSerializer
mp.messaging.outgoing.restaurant-rejected.connector=smallrye-kafka
mp.messaging.outgoing.restaurant-rejected.topic=restaurant-rejected
mp.messaging.outgoing.restaurant-rejected.value.serializer=io.quarkus.kafka.client.serialization.ObjectMapperSerializer
# Consumer → receive order
mp.messaging.incoming.restaurant-request.connector=smallrye-kafka
mp.messaging.incoming.restaurant-request.topic=restaurant-request
mp.messaging.incoming.restaurant-request.group.id=restaurant-service
mp.messaging.incoming.restaurant-request.failure-strategy=dead-letter-queue
mp.messaging.incoming.restaurant-request.dead-letter-queue.topic=restaurant-request-dlq
mp.messaging.incoming.restaurant-request.max-retries=5
mp.messaging.incoming.restaurant-request.retry-backoff.initial=1s
mp.messaging.incoming.restaurant-request.retry-backoff.max=30s
mp.messaging.incoming.restaurant-request.retry-backoff.factor=2.0
mp.messaging.incoming.restaurant-request.value.deserializer=io.quarkus.kafka.client.serialization.ObjectMapperDeserializer
mp.messaging.incoming.restaurant-request.value.deserializer.value-type=com.example.shared.events.RestaurantRequestEvent

📥 4.3 Consumer + Producer
import io.quarkus.logging.Log;
@ApplicationScoped
public class RestaurantEventConsumer {

    @Channel("restaurant-approved")
    Emitter<RestaurantApprovedEvent> approvedEmitter;

    @Channel("restaurant-rejected")
    Emitter<RestaurantRejectedEvent> rejectedEmitter;

    @Incoming("restaurant-request")
    public void process(RestaurantRequestEvent event) {

        boolean accepted = false; // simulate failure

        if (accepted) {
            Log.info("RESTAURANT ACCEPTED");

            approvedEmitter.send(
                Message.of(new RestaurantApprovedEvent.of(event.orderId()))
					.addMetadata(key(event.orderId()))
            );
        } else {
            Log.info("RESTAURANT REJECTED");

            rejectedEmitter.send(
                Message.of(new RestaurantRejectedEvent.of(event.orderId(), "No capacity"))
					.addMetadata(key(event.orderId()))
            );
        }	
    }
	
	private OutgoingKafkaRecordMetadata<UUID> key(UUID orderId) {
		return OutgoingKafkaRecordMetadata.<UUID>builder()
				.withKey(orderId)
				.build();
	}
}
