/*
 * Copyright (c) 2013, The Linux Foundation. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *   * Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *   * Redistributions in binary form must reproduce the above
 *     copyright notice, this list of conditions and the following
 *     disclaimer in the documentation and/or other materials provided
 *     with the distribution.
 *   * Neither the name of The Linux Foundation nor the names of its
 *     contributors may be used to endorse or promote products derived
 *     from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED "AS IS" AND ANY EXPRESS OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NON-INFRINGEMENT
 * ARE DISCLAIMED.  IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS
 * BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR
 * BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
 * OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN
 * IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package android.bluetooth;

import android.annotation.SdkConstant;
import android.annotation.SdkConstant.SdkConstantType;
import android.content.Context;
import android.os.Binder;
import android.os.IBinder;
import android.os.Message;
import android.os.ParcelUuid;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.bluetooth.BluetoothLEServiceUuid;
import android.bluetooth.IQBluetoothAdapterCallback;
import android.util.Log;
import android.util.Pair;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

/**
 * Represents the local device BLE adapter. The {@link QBluetoothAdapter}
 * lets you perform fundamental Bluetooth tasks, such as initiate
 * LE device discovery based on service filters, stop LE scan (with filter)
 * register and remove LPP clients.
 *
 * <p>To get a {@link QBluetoothAdapter} representing this specific Bluetooth
 * adapter, call the
 * static {@link #getDefaultAdapter} method; when running on JELLY_BEAN_MR2 and
 * higher. Once you have the local adapter, you can get a set of
 * {@link BluetoothDevice} objects representing LE devices found that meet
 * a specific LE scan filter criterea based on service UUIDs.
 */
 /** @hide */
public final class QBluetoothAdapter {
    private static final String TAG = "QBluetoothAdapter";
    private static final boolean DBG = false;
    private static final boolean VDBG = false;

    /** @hide */
    public static final int ADV_MODE_NONE = 24;
    /** @hide */
    public static final int ADV_IND_GENERAL_CONNECTABLE=25;
    /** @hide */
    public static final int ADV_IND_LIMITED_CONNECTABLE=26;
    /** @hide */
    public static final int ADV_DIR_CONNECTABLE=27;
   /**
     * Broadcast Action: The local QBluetooth adapter has changed the adv enable mode
     *which can be anything from adv_ind_limited, adv_ind_general, adv_directed or adv_none
     *<p> The adv type determines the way the remote devices can see and connect to the local adapter
     *<p> Always contains the extra field {@link #EXTRA_ADV_TYPE} containing
     *the type of adv currently active
     *<p> Requires {@link android.Manifest.permission#BLUETOOTH} to receive.
     */
    /** @hide */
    @SdkConstant(SdkConstantType.BROADCAST_INTENT_ACTION)
    public static final String ACTION_ADV_ENABLE_CHANGED=
            "android.bluetooth.adapter.action.ADV_ENABLE_CHANGED";
    /**
     * Broadcast Action: Broadcasts an intent for BLE connection
     * parameter update complete/remote connection parameter request events
     * <p>Always contains the extra field {@link #EXTRA_DEVICE}.
     * <p>Always contains the extra field {@link #CONN_INTERVAL_MIN}.
     * <p>Always contains the extra field {@link #CONN_INTERVAL_MAX}.
     * <p>Always contains the extra field {@link #CONN_LATENCY}.
     * <p>Always contains the extra field {@link #SUPERVISION_TIMEOUT}.
     * <p>Always contains the extra field {@link #STATUS}.
     * <p>Always contains the extra field {@link #EVENT}.
     * <p>Requires {@link android.Manifest.permission#BLUETOOTH} to receive.
     * @hide
     */
    @SdkConstant(SdkConstantType.BROADCAST_INTENT_ACTION)
    public static final String ACTION_BLE_CONN_PARAMS =
            "android.bluetooth.adapter.action.ACTION_BLE_CONN_PARAMS";
    /**
     *Used as an extra field in {@link #ACTION_BLE_CONN_PARAMS} intent.
     * @hide
     */
    public static final String EXTRA_CONN_INTERVAL_MIN = "android.bluetooth.adapter.extra.CONN_INTERVAL_MIN";
    /**
     *Used as an extra field in {@link #ACTION_BLE_CONN_PARAMS} intent.
     * @hide
     */
    public static final String EXTRA_CONN_INTERVAL_MAX = "android.bluetooth.adapter.extra.CONN_INTERVAL_MAX";
    /**
     *Used as an extra field in {@link #ACTION_BLE_CONN_PARAMS} intent.
     * @hide
     */
    public static final String EXTRA_CONN_LATENCY = "android.bluetooth.adapter.extra.CONN_LATENCY";
    /**
     *Used as an extra field in {@link #ACTION_BLE_CONN_PARAMS} intent.
     * @hide
     */
    public static final String EXTRA_SUPERVISION_TIMEOUT = "android.bluetooth.adapter.extra.SUPERVISION_TIMEOUT";
    /**
     * Used as an extra field in {@link #ACTION_BLE_CONN_PARAMS} intent.
     * @hide
     */
    public static final String EXTRA_STATUS = "android.bluetooth.adapter.extra.STATUS";
    /**
    * Used as an extra field in {@link #ACTION_BLE_CONN_PARAMS} intent.
     * @hide
     */
    public static final String EXTRA_EVENT = "android.bluetooth.adpater.extra.EVENT";
    /**
     * Used as a String extra field in {@link #ACTION_LOCAL_NAME_CHANGED}
     * intents to request the local Bluetooth name.
     */
    /** @hide */
    public static final String EXTRA_ADV_ENABLE = "android.bluetooth.adapter.extra.ADV_ENABLE";
    private static final String BT_LE_EXTENDED_SCAN_PROP = "ro.q.bluetooth.le.extendedscan";
    private boolean mLeExtendedScanFlag = false;
    private static final int MAX_LE_EXTENDED_SCAN_FILTER_ENTRIES = 0x80;

    private static QBluetoothAdapter sAdapter;
    private static BluetoothAdapter mAdapter;

    private final IBluetoothManager mManagerService;
    private IBluetooth mService;
    private IQBluetooth mQService;
    private Pair<BluetoothAdapter.LeScanCallback, LEExtendedScanClientWrapper> mLeScanClient = null;
    private final Object mScanLock = new Object();

    private final Map<LeLppCallback, LeLppClientWrapper> mLppClients = new HashMap<LeLppCallback, LeLppClientWrapper>();
    /**
     * Get a handle to the default local QBluetooth adapter.
     * <p>Currently Android only supports one QBluetooth adapter, but the API
     * could be extended to support more. This will always return the default
     * adapter.
     * @return the default local adapter, or null if Bluetooth is not supported
     *         on this hardware platform
     */
    public static synchronized QBluetoothAdapter getDefaultAdapter() {
        mAdapter=BluetoothAdapter.getDefaultAdapter();
        IBluetoothManager managerService=mAdapter.getBluetoothManager();
        sAdapter = new QBluetoothAdapter(managerService);
        return sAdapter;
    }

    /**
     * Use {@link #getDefaultAdapter} to get the BluetoothAdapter instance.
     */
    QBluetoothAdapter(IBluetoothManager managerService){//, IQBluetooth QbluetoothBinder) {
        if (managerService == null) {
            throw new IllegalArgumentException("bluetooth manager service is null");
        }
        try {
            //mService = managerService.registerAdapter(mManagerCallback);
            mService=mAdapter.getBluetoothService(mAdapterServiceCallback);
           //mQService=managerService.getQBluetooth();
           mQService=managerService.registerQAdapter(mManagerCallback);
           Log.i(TAG,"mQService= :" + mQService);
        } catch (RemoteException e) {Log.e(TAG, "", e);}
        mManagerService = managerService;
        /* read property value to check if the bluetooth controller support le extended scan */
        String value = (String)System.getProperty(BT_LE_EXTENDED_SCAN_PROP);
        if(value == null){
            Log.e(TAG, "cannot read property " + BT_LE_EXTENDED_SCAN_PROP);
        }else {
            Log.i(TAG, "property " + BT_LE_EXTENDED_SCAN_PROP + "value="+value);
        }

        /*if(value != null && value.equals("true"))*/{
            mLeExtendedScanFlag = true;
        }
    }
    /**
    * gets the adv mode of LE adapter
    * <p>Requires the {@link android.Manifest.permission#BLUETOOTH_ADMIN}
    * permission
    * @return returns the LE adv mode
    /** @hide */
    public int getLEAdvMode() {
        if (mAdapter.getState() != BluetoothAdapter.STATE_ON) return ADV_MODE_NONE;
        try {
            synchronized(mManagerCallback) {
                if (mService != null && mQService!=null) return mQService.getLEAdvMode();
            }
        } catch (RemoteException e) {Log.e(TAG, "", e);}
        return ADV_MODE_NONE;
    }
    /**
    * sets the adv mode of LE adapter
    * <p>Requires the {@link android.Manifest.permission#BLUETOOTH_ADMIN}
    * permission
    * @return returns true if operation successful
    /** @hide */
    public boolean setLEAdvMode(int mode) {
        if (mAdapter.getState() != BluetoothAdapter.STATE_ON) return false;
        try {
            synchronized(mManagerCallback) {
                if (mService != null && mQService!=null)
                {
                    Log.v(TAG,"setLEAdvMode gng to call set LE adv mode Q");
                    return mQService.setLEAdvMode(mode);
                }
            }
        } catch (RemoteException e) {Log.e(TAG, "", e);}
        return false;
    }
    /**
    * sets the adv parameters of LE adapter
    * <p>Requires the {@link android.Manifest.permission#BLUETOOTH_ADMIN}
    * permission
    * @return returns true if operation successful
    /** @hide */
    public boolean setLEAdvParams(int min_int, int max_int, String address, int ad_type){
        if (mAdapter.getState() != BluetoothAdapter.STATE_ON) return false;
        Log.v(TAG, "QBluetooth adapter, setLEAdvParams calling service method min_int"
           + min_int +" max int:"+max_int + "address:" +address + " ad_type="+ ad_type);
        try {
            synchronized(mManagerCallback) {
                if (mService!=null && mQService != null) return mQService.setLEAdvParams(min_int, max_int, address, ad_type);
            }
        } catch (RemoteException e) {Log.e(TAG, "", e);}
        return false;
    }

    /**
    * sets the adv params of LE adapter
    * <p>Requires the {@link android.Manifest.permission#BLUETOOTH_ADMIN}
    * permission
    * @return returns true if operation successful
    /** @hide */
    public boolean setLEAdvParams(int min_int, int max_int){
        if (mAdapter.getState() != BluetoothAdapter.STATE_ON) return false;
        int ad_type=0;
        String address="00:00:00:00:00:00"; //null address
        Log.v(TAG, "QBluetooth adapter, setLEAdvParams min_int" + min_int +" max int:"+max_int + "address:" +address + " ad_type="+ ad_type);
        return setLEAdvParams(min_int, max_int, address, ad_type);
    }

    /**
    * sets the manufacturing data for advertisements of LE adapter
    * <p>Requires the {@link android.Manifest.permission#BLUETOOTH_ADMIN}
    * permission
    * @return returns true if operation successful
    /** @hide */
    public boolean setLEManuData(byte[] manuData){
        if (mAdapter.getState() != BluetoothAdapter.STATE_ON) return false;
        Log.v(TAG, "QBluetooth adapter, setLEManuData calling service method manu_data:" + manuData);
        try {
            synchronized(mManagerCallback) {
                if (mQService != null) return mQService.setLEManuData(manuData);
            }
        } catch (RemoteException e) {Log.e(TAG, "", e);}
        return false;
    }
    /**
    * sets the service data for advertisements of LE adapter
    * <p>Requires the {@link android.Manifest.permission#BLUETOOTH_ADMIN}
    * permission
    * @return returns true if operation successful
    /** @hide */
    public boolean setLEServiceData(byte[] serviceData){
        if (mAdapter.getState() != BluetoothAdapter.STATE_ON) return false;
        Log.v(TAG, "QBluetooth adapter, setLEServiceData calling service method setLEServiceData:" + serviceData);
        try {
            synchronized(mManagerCallback) {
                if (mQService != null) return mQService.setLEServiceData(serviceData);
            }
        } catch (RemoteException e) {Log.e(TAG, "", e);}
        return false;
    }

    /**
    * sets the adv mask for advertisements of LE adapter
    * <p>Requires the {@link android.Manifest.permission#BLUETOOTH_ADMIN}
    * permission
    * @return returns true if operation successful
    /** @hide */
    public boolean setLEAdvMask(boolean bLocalName, boolean bServices, boolean bTxPower,boolean bManuData, boolean ServiceData){
        if (mAdapter.getState() != BluetoothAdapter.STATE_ON) return false;
        Log.v(TAG, "QBluetooth adapter, setLEAdvMask calling service method blocalname:" + bLocalName + " bServices:" + bServices + " bTxPower:"+bTxPower + " bManuData:" + bManuData);
        try {
            synchronized(mManagerCallback) {
                if (mQService != null) return mQService.setLEAdvMask(bLocalName,bServices,bTxPower,bManuData, ServiceData);
            }
        } catch (RemoteException e) {Log.e(TAG, "", e);}
        return false;
    }

    /**
    * sets the adv mask for scan response of LE adapter
    * <p>Requires the {@link android.Manifest.permission#BLUETOOTH_ADMIN}
    * permission
    * @return returns true if operation successful
    /** @hide */
    public boolean setLEScanRespMask(boolean bLocalName, boolean bServices, boolean bTxPower,boolean bManuData){
        if (mAdapter.getState() != BluetoothAdapter.STATE_ON) return false;
        Log.v(TAG, "QBluetooth adapter, setLEScanRespMask calling service method blocalname:" + bLocalName + " bServices:" + bServices + " bTxPower:"+bTxPower+ " bManuData:" + bManuData);
        try {
            synchronized(mManagerCallback) {
                if (mQService != null) return mQService.setLEScanRespMask(bLocalName,bServices,bTxPower,bManuData);
            }
        } catch (RemoteException e) {Log.e(TAG, "", e);}
        return false;
    }
    /**
     * Starts a scan for Bluetooth LE devices, looking for devices that
     * advertise given services.It is the most optimal way to filter scan
     * results.This function can only be invoked if the chipset supports
     * filtering scan results in controller.Otherwise, it is supposed to
     * invoke {@link BluetoothAdapter#startLeScan}.Unlike startLeScan,which
     * filters scan results by ANDing all the services in host, this function
     * filters scan results by ORed all the services(up to 128 services) in
     * controller.For this function only one scan session is allowed at the same time,
     * that means you must make sure there is no scan session in progress before you
     * invoke this function,and you must terminate this current scan session before
     * you can initiate a new scan session.
     *
     * <p>Devices which advertise any service specified are reported using the
     * {@link BluetoothAdapter#LeScanCallback#onLeScan} callback.
     *
     * <p>Requires {@link android.Manifest.permission#BLUETOOTH_ADMIN} permission.
     *
     * @param serviceUuids Array of services used to filter advertising data
     * @param callback the callback LE scan results are delivered
     * @return true, if the scan was started successfully
     *         false,if parameter is bad or chipset doesn't support filtering scan results in controller
     */
    public boolean startLeScanEx(BluetoothLEServiceUuid[] serviceUuids, BluetoothAdapter.LeScanCallback callback) {
        if (VDBG) Log.v(TAG, "startLeScanEx(): " + serviceUuids);
        if (!mLeExtendedScanFlag) {
            if (DBG) Log.e(TAG, "startLeScanEx: function is diabled since chipset doesn't support filtering" +
                                 "scan results in controller, use startLeScan instead");
            return false;
        }

        if(callback == null) {
            if (VDBG) Log.e(TAG, "startLeScanEx: null callback");
            return false;
        }

        /* serviceUuuids cannot be empty set  */
        if ((serviceUuids == null) || (0 == serviceUuids.length) ||
            (serviceUuids.length > MAX_LE_EXTENDED_SCAN_FILTER_ENTRIES))

        {
            if (DBG) Log.e(TAG, "startLeScanEx: invalid serviceUuids array");
            return false;
        }

        synchronized(mScanLock) {
            if (mLeScanClient != null) {
                if (VDBG) Log.v(TAG, "LE Scan in progress");
                if (mLeScanClient.first == callback)
                    if (DBG) Log.v(TAG, "duplicate scan request");
                return false;
            }

            if (mQService == null) {
                if (DBG) Log.e(TAG, "QBluetooth Adapter service not supported");
                return false;
            }

            LEExtendedScanClientWrapper wrapper = new LEExtendedScanClientWrapper(this, mQService, serviceUuids, callback);
            if (wrapper != null && wrapper.startScan()) {
                mLeScanClient = new Pair<BluetoothAdapter.LeScanCallback, LEExtendedScanClientWrapper>(callback, wrapper);
                if (mLeScanClient == null) {
                    if (DBG) Log.d(TAG, "no resource");
                    wrapper.stopScan();
                    return false;
                }
                return true;
            } else {
                if (DBG) Log.e(TAG, "LE scan does not start successfully");
            }
            return false;
        }
    }

    /**
     * Stops an ongoing Bluetooth LE device scan.
     *
     * <p>Requires {@link android.Manifest.permission#BLUETOOTH_ADMIN} permission.
     *
     * @param callback used to identify which scan to stop
     *        must be the same handle used to start the scan
     */
    public void stopLeScanEx(BluetoothAdapter.LeScanCallback callback) {
        synchronized(mScanLock) {
            if(mLeScanClient != null && mLeScanClient.first == callback) {
                LEExtendedScanClientWrapper wrapper = mLeScanClient.second;
                if(wrapper != null) {
                    wrapper.stopScan();
                }
                mLeScanClient = null;
            }
        }
    }

    interface LeLppCallback {
        public void onWriteRssiThreshold(int status);

        public void onReadRssiThreshold(int low,int upper, int alert, int status);

        public void onEnableRssiMonitor(int enable, int status);

        public void onRssiThresholdEvent(int evtType, int rssi);

        public boolean onUpdateLease();
    };

    public boolean registerLppClient(LeLppCallback client, String address, boolean add) {
        synchronized(mLppClients) {
            if (add) {
                if(mLppClients.containsKey(client)) {
                    Log.e(TAG, "Lpp Client has been already registered");
                    return false;
                }

                LeLppClientWrapper wrapper = new LeLppClientWrapper(this, mQService, address, client);
                if(wrapper != null && wrapper.register2(true)) {
                    mLppClients.put(client, wrapper);
                    return true;
                }
                return false;
            } else {
                LeLppClientWrapper wrapper = mLppClients.remove(client);
                if (wrapper != null) {
                    wrapper.register2(false);
                    return true;
                }
                return false;
            }
        }
    }

   /**
     * Write the rssi threshold for a connected remote device.
     *
     * <p>The {@link BluetoothGattCallback#onWriteRssiThreshold} callback will be
     * invoked when the write request has been sent.
     *
     * <p>Requires {@link android.Manifest.permission#BLUETOOTH} permission.
     *
     * @param min    The lower limit of rssi threshold
     * @param max    The upper limit of rssi threshold
     * @return true, if the rssi threshold writing request has been sent tsuccessfully
     */
    public boolean writeRssiThreshold(LeLppCallback client, int min, int max) {
        LeLppClientWrapper wrapper = null;
        synchronized(mLppClients) {
            wrapper = mLppClients.get(client);
        }
        if (wrapper == null) return false;

        wrapper.writeRssiThreshold((byte)min, (byte)max);
        return true;
    }

    /**
     * Enable rssi monitoring for a connected remote device.
     *
     * <p>The {@link BluetoothGattCallback#onEnableRssiMonitor} callback will be
     * invoked when the rssi monitor enable/disable request has been sent.
     *
     * <p>Requires {@link android.Manifest.permission#BLUETOOTH} permission.
     *
     * @param enable disable/enable rssi monitor
     * @return true, if the rssi monitoring request has been sent tsuccessfully
     */
    public boolean enableRssiMonitor(LeLppCallback client, boolean enable){
        LeLppClientWrapper wrapper = null;
        synchronized(mLppClients) {
            wrapper = mLppClients.get(client);
        }
        if (wrapper == null) return false;

        wrapper.enableMonitor(enable);
        return true;
    }

   /**
     * Read the rssi threshold for a connected remote device.
     *
     * <p>The {@link BluetoothGattCallback#onReadRssiThreshold} callback will be
     * invoked when the rssi threshold has been read.
     *
     * <p>Requires {@link android.Manifest.permission#BLUETOOTH} permission.
     *
     * @return true, if the rssi threshold has been requested successfully
     */
    public boolean readRssiThreshold(LeLppCallback client) {
        LeLppClientWrapper wrapper = null;
        synchronized(mLppClients) {
            wrapper = mLppClients.get(client);
        }
        if (wrapper == null) return false;

        wrapper.readRssiThreshold();
        return true;
    }

    /**
     * sends LE Conn update
     * <p>Requires the {@link android.Manifest.permission#BLUETOOTH_ADMIN}
     * permission
     * @return returns true if operation successful
     /** @hide */
     public boolean sendLEConnUpdate(BluetoothDevice device, int interval_min, int interval_max,
             int latency, int supervisionTimeout){
         if (mAdapter.getState() != BluetoothAdapter.STATE_ON)
             return false;
         Log.v(TAG, "QBluetooth adapter, sendLEConnUpdate interval_min" + interval_min +"" +
                 " max interval_max:"+interval_max + "latency:" +latency + " supervisionTimeout="+ supervisionTimeout);
         try {
             synchronized(mManagerCallback) {
                 if (mService!=null && mQService != null)
                     return mQService.sendLEConnUpdate(device, interval_min, interval_max,
                             latency, supervisionTimeout);
             }
         } catch (RemoteException e) {Log.e(TAG, "sendLEConnUpdate", e);}
         return false;
     }

    protected void finalize() throws Throwable {
        try {
            mManagerService.unregisterQAdapter(mManagerCallback);
        } catch (RemoteException e) {
            Log.e(TAG, "", e);
        } finally {
            super.finalize();
        }
    }

    private static class LEExtendedScanClientWrapper extends IQBluetoothAdapterCallback.Stub {
        private final WeakReference<QBluetoothAdapter> mAdapter;
        private final IQBluetooth mQBluetoothAdapterService;
        private final BluetoothLEServiceUuid[] mServiceFilter;
        private final BluetoothAdapter.LeScanCallback mClient;
        private int mScanToken;

        public LEExtendedScanClientWrapper (QBluetoothAdapter adapter, IQBluetooth adapterService,
                                                BluetoothLEServiceUuid[] services, BluetoothAdapter.LeScanCallback callback) {
            this.mAdapter = new WeakReference<QBluetoothAdapter> (adapter);
            this.mQBluetoothAdapterService = adapterService;
            this.mServiceFilter = services;
            this.mClient = callback;
            this.mScanToken = -1;
        }

        public boolean startScan() {
            boolean started = false;
            synchronized(this) {
                if(mScanToken == -1 &&
                mQBluetoothAdapterService != null) {
                    try {
                        mScanToken =  mQBluetoothAdapterService.startLeScanEx(mServiceFilter, this);
                        if(mScanToken != -1) {
                            started = true;
                        }
                    } catch (RemoteException e) {
                        Log.e(TAG, "", e);
                        started = false;
                    }
                }
            }
            return started;
        }

        public void stopScan() {
            synchronized(this) {
                if(mScanToken != -1 &&
                    mQBluetoothAdapterService != null) {
                    try {
                        mQBluetoothAdapterService.stopLeScanEx(mScanToken);
                    } catch (RemoteException e) {
                        Log.e(TAG, "", e);
                    }
                    mScanToken = -1;
                }
            }
        }
        /**
         * Callback reporting LE extended scan result.
         * @hide
         */
        public void onScanResult(String address, int rssi, byte[] advData) {
            synchronized(this) {
                if(mScanToken <= 0) return;
            }
            try {
                QBluetoothAdapter adapter = mAdapter.get();
                if(adapter == null){
                    Log.e(TAG, "OnScanResult, QBluetoothAdapter null");
                    return;
                }
                mClient.onLeScan(BluetoothAdapter.getDefaultAdapter().getRemoteDevice(address), rssi, advData);
            } catch (Exception e) {
                Log.w(TAG, "Unhandled exception: " + e);
            }
        }

        public void onWriteRssiThreshold(String address, int status) {
        }

        public void onReadRssiThreshold(String address, int low, int upper,
                                        int alert, int status) {
        }

        public void onEnableRssiMonitor(String address, int enable, int status) {
        }

        public void onRssiThresholdEvent(String address, int evtType, int rssi) {
        }

        public boolean onUpdateLease() {
            QBluetoothAdapter adapter = mAdapter.get();
            if(adapter == null){
                Log.e(TAG, "onUpdateLease(), QBluetoothAdapter null");
                return false;
            }
            return true;
        }
    }

    private static class LeLppClientWrapper extends IQBluetoothAdapterCallback.Stub {
            private final WeakReference<QBluetoothAdapter> mAdapter;
            private final IQBluetooth mQBluetoothAdapterService;
            private final String mDevice;
            private final LeLppCallback client;

            public LeLppClientWrapper (QBluetoothAdapter adapter, IQBluetooth adapterService,
                                       String address, LeLppCallback callback) {
                this.mAdapter = new WeakReference<QBluetoothAdapter> (adapter);
                this.mQBluetoothAdapterService = adapterService;
                this.mDevice = address;
                this.client = callback;
            }

            public boolean register2(boolean add) {
                if(mQBluetoothAdapterService != null) {
                    try {
                        return mQBluetoothAdapterService.registerLeLppRssiMonitorClient(mDevice, this, add);
                    } catch (RemoteException e) {
                        Log.w(TAG, "", e);
                    }
                }
                return false;
            }

            public void writeRssiThreshold(byte min, byte max) {
                if(mQBluetoothAdapterService != null) {
                    try {
                        mQBluetoothAdapterService.writeLeLppRssiThreshold(mDevice, min, max);
                    } catch (RemoteException e) {
                        Log.w(TAG, "", e);
                    }
                }
            }

            public void enableMonitor(boolean enable) {
                if(mQBluetoothAdapterService != null) {
                    try {
                        mQBluetoothAdapterService.enableLeLppRssiMonitor(mDevice, enable);
                    } catch (RemoteException e) {
                        Log.w(TAG, "", e);
                    }
                }
            }

            public void readRssiThreshold() {
                if(mQBluetoothAdapterService != null) {
                    try {
                        mQBluetoothAdapterService.readLeLppRssiThreshold(mDevice);
                    } catch (RemoteException e) {
                        Log.w(TAG, "", e);
                    }
                }
            }

            public void onScanResult(String address, int rssi, byte[] advData) {
            }

           /**
             * Rssi threshold has been written
             * @hide
             */
            public void onWriteRssiThreshold(String address, int status){
                if (client == null) {
                    return;
                }
                try {
                    client.onWriteRssiThreshold(status);
                } catch (Exception ex) {
                    Log.w(TAG, "Unhandled exception: " + ex);
                }
            }

            /**
             * RSSI threshold has been read
             * @hide
             */
            public void onReadRssiThreshold(String address, int low, int upper,
                                            int alert, int status){
                if (client == null) {
                    return;
                }
                try {
                    client.onReadRssiThreshold(low, upper, alert, status);
                } catch (Exception ex) {
                    Log.w(TAG, "Unhandled exception: " + ex);
                }
            }

           /**
             * Remote Device RSSI monitoring has been enabled/disabled
             * @hide
             */
            public void onEnableRssiMonitor(String address, int enable, int status){
                if (client == null) {
                    return;
                }
                try {
                    client.onEnableRssiMonitor(enable, status);
                } catch (Exception ex) {
                    Log.w(TAG, "Unhandled exception: " + ex);
                }
            }

            /**
             * RSSI threshold event reported
             * @hide
             */
            public void onRssiThresholdEvent(String address, int evtType, int rssi){
                if (client == null) {
                    return;
                }
                try {
                    client.onRssiThresholdEvent(evtType, rssi);
                } catch (Exception ex) {
                    Log.w(TAG, "Unhandled exception: " + ex);
                }
            }

            public boolean onUpdateLease() {
                if (client == null) return false;
                try {
                    return client.onUpdateLease();
                } catch (Exception ex) {
                    Log.w(TAG, "Unhandled exception: " + ex);
                    return false;
                }
            }
    }

    final private IBluetoothManagerCallback mAdapterServiceCallback =
        new IBluetoothManagerCallback.Stub() {
            public void onBluetoothServiceUp(IBluetooth bluetoothService) {
                synchronized (mAdapterServiceCallback) {
                    //initialize the global params again
                    mService = bluetoothService;
                    Log.i(TAG,"onBluetoothServiceUp Adapter ON: mService: "+ mService + " mQService: " + mQService+ " ManagerService:" + mManagerService);
                }
            }

            public void onBluetoothServiceDown() {
                synchronized (mAdapterServiceCallback) {
                    mService = null;
                    Log.i(TAG,"onBluetoothServiceDown Adapter OFF: mService: "+ mService + " mQService: " + mQService);
                }
            }
    };


    final private IQBluetoothManagerCallback mManagerCallback =
        new IQBluetoothManagerCallback.Stub() {
            public void onQBluetoothServiceUp(IQBluetooth qbluetoothService) {
                if (VDBG) Log.i(TAG, "on QBluetoothServiceUp: " + qbluetoothService);
                synchronized (mManagerCallback) {
                    //initialize the global params again
                    mQService = qbluetoothService;
                    Log.i(TAG,"onQBluetoothServiceUp: Adapter ON: mService: "+ mService + " mQService: " + mQService+ " ManagerService:" + mManagerService);
                }
            }

            public void onQBluetoothServiceDown() {
                if (VDBG) Log.i(TAG, "onQBluetoothServiceDown: " + mService);
                synchronized (mManagerCallback) {
                    mQService=null;
                    Log.i(TAG,"onQBluetoothServiceDown: Adapter OFF: mService: "+ mService + " mQService: " + mQService);
                }
            }
    };

}
