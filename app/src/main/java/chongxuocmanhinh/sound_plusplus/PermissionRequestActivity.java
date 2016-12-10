package chongxuocmanhinh.sound_plusplus;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;

/**
 * Created by L on 10/12/2016.
 * Class này để cho người dùng xác nhận các quyền mà mình khai báo trên manifest.xml
 * Ví dụ việc ứng dụng có được truy xuất bọ nhớ sdcard (READ_EXTERNAL_STORAGE) từ bản
 * android 6.0 trở lên
 */
public class PermissionRequestActivity extends Activity {

    /**
     * quyền truy xuất cần người dùng cho phép tử bản android 6.0 trở lên >= M
     */
    private static final String[] NEEDED_PERMISSIONS = { Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE };

    /**
     * The intent to start after acquiring the required permissions
     */
    private Intent mCallbackIntent;

    /**
     * Chỉ cần hiện bảng request cho các thiết bị có API >= 23
     * @param savedInstanceState
     */
    @TargetApi(Build.VERSION_CODES.M)
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mCallbackIntent = getIntent().getExtras().getParcelable("callbackIntent");
        requestPermissions(NEEDED_PERMISSIONS, 0);
    }

    /**
     * Hàm này được gọi bởi activity sau khi user tương tác với permission request
     * Sẽ khởi động main activity nếu tất cả các permission được granted,nếu không thì exit
     *
     * @param requestCode
     * @param permissions
     * @param grantResults
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        //super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        int grantedPermissions = 0;
        for(int result : grantResults){
            if(result == PackageManager.PERMISSION_GRANTED)
                grantedPermissions++;
        }

        finish();

        if (grantedPermissions == grantResults.length) {
            if (mCallbackIntent != null) {
                // start the old intent but ensure to make it a new task & clear any old attached activites
                mCallbackIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(mCallbackIntent);
            }
            // Hack: We *kill* ourselfs (while launching the main activity) to get startet
            // in a new process: This works around a bug/feature in 6.0 that would cause us
            // to get 'partial read' permissions (eg: reading from the content provider works
            // but reading from /sdcard doesn't)
            android.os.Process.killProcess(android.os.Process.myPid());
        }
    }

}
