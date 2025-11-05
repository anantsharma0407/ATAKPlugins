/*
 * This file is auto-generated.  DO NOT MODIFY.
 * Using: /Users/anant/Library/Android/sdk/build-tools/35.0.0/aidl -p/Users/anant/Library/Android/sdk/platforms/android-35/framework.aidl -o/Users/anant/ATAK/ATAK-CIV-5.5.1.8-SDK/plugins/helloworld/app/build/generated/aidl_source_output_dir/civDebug/out -I/Users/anant/ATAK/ATAK-CIV-5.5.1.8-SDK/plugins/helloworld/app/src/main/aidl -I/Users/anant/ATAK/ATAK-CIV-5.5.1.8-SDK/plugins/helloworld/app/src/civ/aidl -I/Users/anant/ATAK/ATAK-CIV-5.5.1.8-SDK/plugins/helloworld/app/build-types/debug/aidl -I/Users/anant/ATAK/ATAK-CIV-5.5.1.8-SDK/plugins/helloworld/app/src/civDebug/aidl -d/var/folders/46/8tqz77c11jvddt9zls92l_wh0000gn/T/aidl2836086647576408705.d /Users/anant/ATAK/ATAK-CIV-5.5.1.8-SDK/plugins/helloworld/app/src/main/aidl/com/atakmap/android/helloworld/aidl/ILogger.aidl
 */
package com.atakmap.android.helloworld.aidl;
// Declare any non-default types here with import statements
public interface ILogger extends android.os.IInterface
{
  /** Default implementation for ILogger. */
  public static class Default implements com.atakmap.android.helloworld.aidl.ILogger
  {
    @Override public void e(java.lang.String tag, java.lang.String msg, java.lang.String exception) throws android.os.RemoteException
    {
    }
    @Override public void d(java.lang.String tag, java.lang.String msg, java.lang.String exception) throws android.os.RemoteException
    {
    }
    @Override
    public android.os.IBinder asBinder() {
      return null;
    }
  }
  /** Local-side IPC implementation stub class. */
  public static abstract class Stub extends android.os.Binder implements com.atakmap.android.helloworld.aidl.ILogger
  {
    /** Construct the stub at attach it to the interface. */
    @SuppressWarnings("this-escape")
    public Stub()
    {
      this.attachInterface(this, DESCRIPTOR);
    }
    /**
     * Cast an IBinder object into an com.atakmap.android.helloworld.aidl.ILogger interface,
     * generating a proxy if needed.
     */
    public static com.atakmap.android.helloworld.aidl.ILogger asInterface(android.os.IBinder obj)
    {
      if ((obj==null)) {
        return null;
      }
      android.os.IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
      if (((iin!=null)&&(iin instanceof com.atakmap.android.helloworld.aidl.ILogger))) {
        return ((com.atakmap.android.helloworld.aidl.ILogger)iin);
      }
      return new com.atakmap.android.helloworld.aidl.ILogger.Stub.Proxy(obj);
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
        case TRANSACTION_e:
        {
          java.lang.String _arg0;
          _arg0 = data.readString();
          java.lang.String _arg1;
          _arg1 = data.readString();
          java.lang.String _arg2;
          _arg2 = data.readString();
          this.e(_arg0, _arg1, _arg2);
          reply.writeNoException();
          break;
        }
        case TRANSACTION_d:
        {
          java.lang.String _arg0;
          _arg0 = data.readString();
          java.lang.String _arg1;
          _arg1 = data.readString();
          java.lang.String _arg2;
          _arg2 = data.readString();
          this.d(_arg0, _arg1, _arg2);
          reply.writeNoException();
          break;
        }
        default:
        {
          return super.onTransact(code, data, reply, flags);
        }
      }
      return true;
    }
    private static class Proxy implements com.atakmap.android.helloworld.aidl.ILogger
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
      @Override public void e(java.lang.String tag, java.lang.String msg, java.lang.String exception) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeString(tag);
          _data.writeString(msg);
          _data.writeString(exception);
          boolean _status = mRemote.transact(Stub.TRANSACTION_e, _data, _reply, 0);
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
      @Override public void d(java.lang.String tag, java.lang.String msg, java.lang.String exception) throws android.os.RemoteException
      {
        android.os.Parcel _data = android.os.Parcel.obtain();
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
          _data.writeInterfaceToken(DESCRIPTOR);
          _data.writeString(tag);
          _data.writeString(msg);
          _data.writeString(exception);
          boolean _status = mRemote.transact(Stub.TRANSACTION_d, _data, _reply, 0);
          _reply.readException();
        }
        finally {
          _reply.recycle();
          _data.recycle();
        }
      }
    }
    static final int TRANSACTION_e = (android.os.IBinder.FIRST_CALL_TRANSACTION + 0);
    static final int TRANSACTION_d = (android.os.IBinder.FIRST_CALL_TRANSACTION + 1);
  }
  /** @hide */
  public static final java.lang.String DESCRIPTOR = "com.atakmap.android.helloworld.aidl.ILogger";
  public void e(java.lang.String tag, java.lang.String msg, java.lang.String exception) throws android.os.RemoteException;
  public void d(java.lang.String tag, java.lang.String msg, java.lang.String exception) throws android.os.RemoteException;
}
