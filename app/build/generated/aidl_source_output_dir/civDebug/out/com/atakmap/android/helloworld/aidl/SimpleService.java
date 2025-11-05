/*
 * This file is auto-generated.  DO NOT MODIFY.
 * Using: /Users/anant/Library/Android/sdk/build-tools/35.0.0/aidl -p/Users/anant/Library/Android/sdk/platforms/android-35/framework.aidl -o/Users/anant/ATAK/ATAK-CIV-5.5.1.8-SDK/plugins/helloworld/app/build/generated/aidl_source_output_dir/civDebug/out -I/Users/anant/ATAK/ATAK-CIV-5.5.1.8-SDK/plugins/helloworld/app/src/main/aidl -I/Users/anant/ATAK/ATAK-CIV-5.5.1.8-SDK/plugins/helloworld/app/src/civ/aidl -I/Users/anant/ATAK/ATAK-CIV-5.5.1.8-SDK/plugins/helloworld/app/build-types/debug/aidl -I/Users/anant/ATAK/ATAK-CIV-5.5.1.8-SDK/plugins/helloworld/app/src/civDebug/aidl -d/var/folders/46/8tqz77c11jvddt9zls92l_wh0000gn/T/aidl2830942058640793901.d /Users/anant/ATAK/ATAK-CIV-5.5.1.8-SDK/plugins/helloworld/app/src/main/aidl/com/atakmap/android/helloworld/aidl/SimpleService.aidl
 */
package com.atakmap.android.helloworld.aidl;
public interface SimpleService extends android.os.IInterface
{
  /** Default implementation for SimpleService. */
  public static class Default implements com.atakmap.android.helloworld.aidl.SimpleService
  {
    /**
     * Pass a logging mechanism over to the Service so that the logs can be written to the
     * appropriate logger.
     */
    @Override public void registerLogger(com.atakmap.android.helloworld.aidl.ILogger log) throws android.os.RemoteException
    {
    }
    /** Adds two numbers and returns the result. */
    @Override public int add(int a, int b) throws android.os.RemoteException
    {
      return 0;
    }
    @Override
    public android.os.IBinder asBinder() {
      return null;
    }
  }
  /** Local-side IPC implementation stub class. */
  public static abstract class Stub extends android.os.Binder implements com.atakmap.android.helloworld.aidl.SimpleService
  {
    /** Construct the stub at attach it to the interface. */
    @SuppressWarnings("this-escape")
    public Stub()
    {
      this.attachInterface(this, DESCRIPTOR);
    }
    /**
     * Cast an IBinder object into an com.atakmap.android.helloworld.aidl.SimpleService interface,
     * generating a proxy if needed.
     */
    public static com.atakmap.android.helloworld.aidl.SimpleService asInterface(android.os.IBinder obj)
    {
      if ((obj==null)) {
        return null;
      }
      android.os.IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
      if (((iin!=null)&&(iin instanceof com.atakmap.android.helloworld.aidl.SimpleService))) {
        return ((com.atakmap.android.helloworld.aidl.SimpleService)iin);
      }
      return new com.atakmap.android.helloworld.aidl.SimpleService.Stub.Proxy(obj);
    }
    @Override public android.os.IBinder asBinder()
    {
      return this;
    }
    @Override public boolean onTransact(int code, android.os.Parcel data, android.os.Parcel reply, int flags) throws android.os.RemoteException
    {
      java.lang.String descriptor = DESCRIPTOR;
      if (code >= android.os.IBinder.FIRST_CALL_TRANSACTION && code <= android.os.IBinder.LAST_CALL_TRANSACTION) {
        data.enforceInterface(descriptor);
      }
      if (code == INTERFACE_TRANSACTION) {
        reply.writeString(descriptor);
        return true;
      }
      switch (code)
      {
        case TRANSACTION_registerLogger:
        {
          com.atakmap.android.helloworld.aidl.ILogger _arg0;
          _arg0 = com.atakmap.android.helloworld.aidl.ILogger.Stub.asInterface(data.readStrongBinder());
          this.registerLogger(_arg0);
          reply.writeNoException();
          break;
        }
        case TRANSACTION_add:
        {
          int _arg0;
          _arg0 = data.readInt();
          int _arg1;
          _arg1 = data.readInt();
          int _result = this.add(_arg0, _arg1);
          reply.writeNoException();
          reply.writeInt(_result);
          break;
        }
        default:
        {
          return super.onTransact(code, data, reply, flags);
        }
      }
      return true;
    }
    private static class Proxy implements com.atakmap.android.helloworld.aidl.SimpleService
    {
      private android.os.IBinder mRemote;
      Proxy(android.os.IBinder remote)
      {
        mRemote = remote;
      }
      @Override public android.os.IBinder asBinder()
      {
        return mRemote;
      }
      public java.lang.String getInterfaceDescriptor()
      {
        return DESCRIPTOR;
      }
      /**
       * Pass a logging mechanism over to the Service so that the logs can be written to the
       * appropriate logger.
       */
      @Override public void registerLogger(com.atakmap.android.helloworld.aidl.ILogger log) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeStrongInterface(log);
          boolean _status = mRemote.transact(Stub.TRANSACTION_registerLogger, _data, _reply, 0);
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      /** Adds two numbers and returns the result. */
      @Override public int add(int a, int b) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        android.os.Parcel _reply = android.os.Parcel.obtain();
        int _result;
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeInt(a);
          _data.writeInt(b);
          boolean _status = mRemote.transact(Stub.TRANSACTION_add, _data, _reply, 0);
          _reply.readException();
          _result = _reply.readInt();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
        return _result;
      }
    }
    static final int TRANSACTION_registerLogger = (android.os.IBinder.FIRST_CALL_TRANSACTION + 0);
    static final int TRANSACTION_add = (android.os.IBinder.FIRST_CALL_TRANSACTION + 1);
  }
  /** @hide */
  public static final java.lang.String DESCRIPTOR = "com.atakmap.android.helloworld.aidl.SimpleService";
  /**
   * Pass a logging mechanism over to the Service so that the logs can be written to the
   * appropriate logger.
   */
  public void registerLogger(com.atakmap.android.helloworld.aidl.ILogger log) throws android.os.RemoteException;
  /** Adds two numbers and returns the result. */
  public int add(int a, int b) throws android.os.RemoteException;
}
