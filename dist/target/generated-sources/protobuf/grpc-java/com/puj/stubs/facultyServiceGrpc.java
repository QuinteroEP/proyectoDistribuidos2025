package com.puj.stubs;

import static io.grpc.MethodDescriptor.generateFullMethodName;

/**
 */
@javax.annotation.Generated(
    value = "by gRPC proto compiler (version 1.57.2)",
    comments = "Source: facultad.proto")
@io.grpc.stub.annotations.GrpcGenerated
public final class facultyServiceGrpc {

  private facultyServiceGrpc() {}

  public static final java.lang.String SERVICE_NAME = "facultyService";

  // Static method descriptors that strictly reflect the proto.
  private static volatile io.grpc.MethodDescriptor<com.puj.stubs.Facultad.DatosPeticion,
      com.puj.stubs.Facultad.RespuestaPeticion> getSolicitarSalonMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "solicitarSalon",
      requestType = com.puj.stubs.Facultad.DatosPeticion.class,
      responseType = com.puj.stubs.Facultad.RespuestaPeticion.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.puj.stubs.Facultad.DatosPeticion,
      com.puj.stubs.Facultad.RespuestaPeticion> getSolicitarSalonMethod() {
    io.grpc.MethodDescriptor<com.puj.stubs.Facultad.DatosPeticion, com.puj.stubs.Facultad.RespuestaPeticion> getSolicitarSalonMethod;
    if ((getSolicitarSalonMethod = facultyServiceGrpc.getSolicitarSalonMethod) == null) {
      synchronized (facultyServiceGrpc.class) {
        if ((getSolicitarSalonMethod = facultyServiceGrpc.getSolicitarSalonMethod) == null) {
          facultyServiceGrpc.getSolicitarSalonMethod = getSolicitarSalonMethod =
              io.grpc.MethodDescriptor.<com.puj.stubs.Facultad.DatosPeticion, com.puj.stubs.Facultad.RespuestaPeticion>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "solicitarSalon"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.puj.stubs.Facultad.DatosPeticion.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.puj.stubs.Facultad.RespuestaPeticion.getDefaultInstance()))
              .setSchemaDescriptor(new facultyServiceMethodDescriptorSupplier("solicitarSalon"))
              .build();
        }
      }
    }
    return getSolicitarSalonMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.puj.stubs.Facultad.Empty,
      com.puj.stubs.Facultad.Empty> getPopulateListsMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "populateLists",
      requestType = com.puj.stubs.Facultad.Empty.class,
      responseType = com.puj.stubs.Facultad.Empty.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.puj.stubs.Facultad.Empty,
      com.puj.stubs.Facultad.Empty> getPopulateListsMethod() {
    io.grpc.MethodDescriptor<com.puj.stubs.Facultad.Empty, com.puj.stubs.Facultad.Empty> getPopulateListsMethod;
    if ((getPopulateListsMethod = facultyServiceGrpc.getPopulateListsMethod) == null) {
      synchronized (facultyServiceGrpc.class) {
        if ((getPopulateListsMethod = facultyServiceGrpc.getPopulateListsMethod) == null) {
          facultyServiceGrpc.getPopulateListsMethod = getPopulateListsMethod =
              io.grpc.MethodDescriptor.<com.puj.stubs.Facultad.Empty, com.puj.stubs.Facultad.Empty>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "populateLists"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.puj.stubs.Facultad.Empty.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.puj.stubs.Facultad.Empty.getDefaultInstance()))
              .setSchemaDescriptor(new facultyServiceMethodDescriptorSupplier("populateLists"))
              .build();
        }
      }
    }
    return getPopulateListsMethod;
  }

  /**
   * Creates a new async stub that supports all call types for the service
   */
  public static facultyServiceStub newStub(io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<facultyServiceStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<facultyServiceStub>() {
        @java.lang.Override
        public facultyServiceStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new facultyServiceStub(channel, callOptions);
        }
      };
    return facultyServiceStub.newStub(factory, channel);
  }

  /**
   * Creates a new blocking-style stub that supports unary and streaming output calls on the service
   */
  public static facultyServiceBlockingStub newBlockingStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<facultyServiceBlockingStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<facultyServiceBlockingStub>() {
        @java.lang.Override
        public facultyServiceBlockingStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new facultyServiceBlockingStub(channel, callOptions);
        }
      };
    return facultyServiceBlockingStub.newStub(factory, channel);
  }

  /**
   * Creates a new ListenableFuture-style stub that supports unary calls on the service
   */
  public static facultyServiceFutureStub newFutureStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<facultyServiceFutureStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<facultyServiceFutureStub>() {
        @java.lang.Override
        public facultyServiceFutureStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new facultyServiceFutureStub(channel, callOptions);
        }
      };
    return facultyServiceFutureStub.newStub(factory, channel);
  }

  /**
   */
  public interface AsyncService {

    /**
     */
    default void solicitarSalon(com.puj.stubs.Facultad.DatosPeticion request,
        io.grpc.stub.StreamObserver<com.puj.stubs.Facultad.RespuestaPeticion> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getSolicitarSalonMethod(), responseObserver);
    }

    /**
     */
    default void populateLists(com.puj.stubs.Facultad.Empty request,
        io.grpc.stub.StreamObserver<com.puj.stubs.Facultad.Empty> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getPopulateListsMethod(), responseObserver);
    }
  }

  /**
   * Base class for the server implementation of the service facultyService.
   */
  public static abstract class facultyServiceImplBase
      implements io.grpc.BindableService, AsyncService {

    @java.lang.Override public final io.grpc.ServerServiceDefinition bindService() {
      return facultyServiceGrpc.bindService(this);
    }
  }

  /**
   * A stub to allow clients to do asynchronous rpc calls to service facultyService.
   */
  public static final class facultyServiceStub
      extends io.grpc.stub.AbstractAsyncStub<facultyServiceStub> {
    private facultyServiceStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected facultyServiceStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new facultyServiceStub(channel, callOptions);
    }

    /**
     */
    public void solicitarSalon(com.puj.stubs.Facultad.DatosPeticion request,
        io.grpc.stub.StreamObserver<com.puj.stubs.Facultad.RespuestaPeticion> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getSolicitarSalonMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void populateLists(com.puj.stubs.Facultad.Empty request,
        io.grpc.stub.StreamObserver<com.puj.stubs.Facultad.Empty> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getPopulateListsMethod(), getCallOptions()), request, responseObserver);
    }
  }

  /**
   * A stub to allow clients to do synchronous rpc calls to service facultyService.
   */
  public static final class facultyServiceBlockingStub
      extends io.grpc.stub.AbstractBlockingStub<facultyServiceBlockingStub> {
    private facultyServiceBlockingStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected facultyServiceBlockingStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new facultyServiceBlockingStub(channel, callOptions);
    }

    /**
     */
    public com.puj.stubs.Facultad.RespuestaPeticion solicitarSalon(com.puj.stubs.Facultad.DatosPeticion request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getSolicitarSalonMethod(), getCallOptions(), request);
    }

    /**
     */
    public com.puj.stubs.Facultad.Empty populateLists(com.puj.stubs.Facultad.Empty request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getPopulateListsMethod(), getCallOptions(), request);
    }
  }

  /**
   * A stub to allow clients to do ListenableFuture-style rpc calls to service facultyService.
   */
  public static final class facultyServiceFutureStub
      extends io.grpc.stub.AbstractFutureStub<facultyServiceFutureStub> {
    private facultyServiceFutureStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected facultyServiceFutureStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new facultyServiceFutureStub(channel, callOptions);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<com.puj.stubs.Facultad.RespuestaPeticion> solicitarSalon(
        com.puj.stubs.Facultad.DatosPeticion request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getSolicitarSalonMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<com.puj.stubs.Facultad.Empty> populateLists(
        com.puj.stubs.Facultad.Empty request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getPopulateListsMethod(), getCallOptions()), request);
    }
  }

  private static final int METHODID_SOLICITAR_SALON = 0;
  private static final int METHODID_POPULATE_LISTS = 1;

  private static final class MethodHandlers<Req, Resp> implements
      io.grpc.stub.ServerCalls.UnaryMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ServerStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ClientStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.BidiStreamingMethod<Req, Resp> {
    private final AsyncService serviceImpl;
    private final int methodId;

    MethodHandlers(AsyncService serviceImpl, int methodId) {
      this.serviceImpl = serviceImpl;
      this.methodId = methodId;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("unchecked")
    public void invoke(Req request, io.grpc.stub.StreamObserver<Resp> responseObserver) {
      switch (methodId) {
        case METHODID_SOLICITAR_SALON:
          serviceImpl.solicitarSalon((com.puj.stubs.Facultad.DatosPeticion) request,
              (io.grpc.stub.StreamObserver<com.puj.stubs.Facultad.RespuestaPeticion>) responseObserver);
          break;
        case METHODID_POPULATE_LISTS:
          serviceImpl.populateLists((com.puj.stubs.Facultad.Empty) request,
              (io.grpc.stub.StreamObserver<com.puj.stubs.Facultad.Empty>) responseObserver);
          break;
        default:
          throw new AssertionError();
      }
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("unchecked")
    public io.grpc.stub.StreamObserver<Req> invoke(
        io.grpc.stub.StreamObserver<Resp> responseObserver) {
      switch (methodId) {
        default:
          throw new AssertionError();
      }
    }
  }

  public static final io.grpc.ServerServiceDefinition bindService(AsyncService service) {
    return io.grpc.ServerServiceDefinition.builder(getServiceDescriptor())
        .addMethod(
          getSolicitarSalonMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.puj.stubs.Facultad.DatosPeticion,
              com.puj.stubs.Facultad.RespuestaPeticion>(
                service, METHODID_SOLICITAR_SALON)))
        .addMethod(
          getPopulateListsMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.puj.stubs.Facultad.Empty,
              com.puj.stubs.Facultad.Empty>(
                service, METHODID_POPULATE_LISTS)))
        .build();
  }

  private static abstract class facultyServiceBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoFileDescriptorSupplier, io.grpc.protobuf.ProtoServiceDescriptorSupplier {
    facultyServiceBaseDescriptorSupplier() {}

    @java.lang.Override
    public com.google.protobuf.Descriptors.FileDescriptor getFileDescriptor() {
      return com.puj.stubs.Facultad.getDescriptor();
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.ServiceDescriptor getServiceDescriptor() {
      return getFileDescriptor().findServiceByName("facultyService");
    }
  }

  private static final class facultyServiceFileDescriptorSupplier
      extends facultyServiceBaseDescriptorSupplier {
    facultyServiceFileDescriptorSupplier() {}
  }

  private static final class facultyServiceMethodDescriptorSupplier
      extends facultyServiceBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoMethodDescriptorSupplier {
    private final java.lang.String methodName;

    facultyServiceMethodDescriptorSupplier(java.lang.String methodName) {
      this.methodName = methodName;
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.MethodDescriptor getMethodDescriptor() {
      return getServiceDescriptor().findMethodByName(methodName);
    }
  }

  private static volatile io.grpc.ServiceDescriptor serviceDescriptor;

  public static io.grpc.ServiceDescriptor getServiceDescriptor() {
    io.grpc.ServiceDescriptor result = serviceDescriptor;
    if (result == null) {
      synchronized (facultyServiceGrpc.class) {
        result = serviceDescriptor;
        if (result == null) {
          serviceDescriptor = result = io.grpc.ServiceDescriptor.newBuilder(SERVICE_NAME)
              .setSchemaDescriptor(new facultyServiceFileDescriptorSupplier())
              .addMethod(getSolicitarSalonMethod())
              .addMethod(getPopulateListsMethod())
              .build();
        }
      }
    }
    return result;
  }
}
