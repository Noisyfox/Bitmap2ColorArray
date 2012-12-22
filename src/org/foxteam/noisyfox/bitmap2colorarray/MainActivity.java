package org.foxteam.noisyfox.bitmap2colorarray;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.Toast;

public class MainActivity extends Activity {

	public static Context mainContext;
	public static ProgressDialog progressDialog;

	public static CheckBox checkbox_batporcess, checkbox_samedir;
	public static EditText edittext_input, edittext_output;
	public static Button button_browse_input, button_browse_output,
			button_process;

	public static final int MESSAGE_PROCESS_START = 1;
	public static final int MESSAGE_PROCESS_FINISH = 2;
	public static final int MESSAGE_SHOW_TOAST = 3;

	public static final Handler mainHandler = new Handler() {

		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case MESSAGE_PROCESS_START:
				progressDialog = new ProgressDialog(mainContext);
				progressDialog.setCancelable(false);
				progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
				progressDialog.setMessage("操作中");
				progressDialog.show();
				break;
			case MESSAGE_PROCESS_FINISH:
				progressDialog.dismiss();
				break;
			case MESSAGE_SHOW_TOAST:
				Toast.makeText(mainContext, (String) msg.obj, Toast.LENGTH_LONG)
						.show();
				break;
			default:
				super.handleMessage(msg);
				break;
			}
		}

	};

	public static final void showDialog(boolean show) {
		Message msg = new Message();
		msg.what = show ? MESSAGE_PROCESS_START : MESSAGE_PROCESS_FINISH;

		mainHandler.sendMessage(msg);
	}

	public static final void showToast(String text) {
		Message msg = new Message();
		msg.what = MESSAGE_SHOW_TOAST;
		msg.obj = text;

		mainHandler.sendMessage(msg);
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		mainContext = this;

		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		checkbox_batporcess = (CheckBox) findViewById(R.id.checkbox_batporcess);
		checkbox_samedir = (CheckBox) findViewById(R.id.checkbox_samedir);
		edittext_input = (EditText) findViewById(R.id.edittext_input);
		edittext_output = (EditText) findViewById(R.id.edittext_output);
		button_browse_input = (Button) findViewById(R.id.button_browse_input);
		button_browse_output = (Button) findViewById(R.id.button_browse_output);
		button_process = (Button) findViewById(R.id.button_process);

		checkbox_samedir
				.setOnCheckedChangeListener(new OnCheckedChangeListener() {

					@Override
					public void onCheckedChanged(CompoundButton arg0,
							boolean arg1) {
						button_browse_output.setEnabled(!arg1);
						edittext_output.setEnabled(!arg1);
					}

				});

		button_process.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				showDialog(true);
				final String inputPath = edittext_input.getText().toString();
				final String outputPath = edittext_output.getText().toString();
				final boolean bat = checkbox_batporcess.isChecked();
				final boolean samedir = checkbox_samedir.isChecked();
				new Thread() {

					@Override
					public void run() {
						try {
							File input_file = new File(inputPath);
							if (!input_file.exists()) {
								throw new Exception("输入文件不存在！");
							}
							if (input_file.isDirectory() && !bat) {
								throw new Exception("当前不是批处理模式！请指定一个输入文件！");
							}
							File output_dir;
							if (samedir) {
								File outf = new File(inputPath);
								if (outf.isDirectory()) {
									output_dir = outf;
								} else {
									output_dir = outf.getParentFile();
								}
							} else {
								output_dir = new File(outputPath);
								if (output_dir.isFile()) {
									throw new Exception("请指定一个输出文件夹！");
								}
							}
							output_dir.mkdirs();
							if (!output_dir.isDirectory()) {
								throw new Exception("创建输出文件夹失败！");
							}

							int count_success = 0;
							int count_fail = 0;
							List<File> bitmapFileList = new ArrayList<File>();
							if (input_file.isFile()) {
								bitmapFileList.add(input_file);
							} else {
								File[] fs = input_file.listFiles();
								for (File f : fs) {
									if (f.isFile()
											&& !f.getName().endsWith(".java"))
										bitmapFileList.add(f);
								}
							}

							for (File inf : bitmapFileList) {
								FileWriter fwriter = null;
								try {
									InputStream is = new FileInputStream(inf);
									Bitmap b = BitmapFactory.decodeStream(is);

									String optFileName = inf.getName()
											+ ".java";

									File optFile = new File(output_dir
											.getPath()
											+ File.separatorChar
											+ optFileName);

									ByteArrayOutputStream baos = new ByteArrayOutputStream();
									b.compress(Bitmap.CompressFormat.PNG, 100,
											baos);

									byte[] bs = baos.toByteArray();
									StringBuffer stb = new StringBuffer();
									stb.append("byte[] "
											+ inf.getName().replace('.', '_')
											+ " = {");
									if (bs.length > 0) {
										for (int i = 0; i < bs.length - 1; i++) {
											stb.append(byte2hex(bs[i]) + ", ");
										}
										stb.append(String
												.valueOf(byte2hex(bs[bs.length - 1])));
									}
									stb.append("};");
									char[] chars = new char[stb.length()];
									stb.getChars(0, stb.length(), chars, 0);

									fwriter = new FileWriter(optFile, false);
									fwriter.write(chars);

									count_success++;
								} catch (Exception ex) {
									ex.printStackTrace();
									count_fail++;
								} finally {
									try {
										fwriter.close();
									} catch (Exception ex) {
										ex.printStackTrace();
									}
								}
							}

							showToast("操作完成," + count_success + "个成功,"
									+ count_fail + "失败");
						} catch (Exception e) {
							showToast(e.getMessage());
						}

						showDialog(false);
					}

				}.start();
			}

		});
	}

	public static final String byte2hex(byte b) {
		String hex = Integer.toHexString(b & 0xFF);
		if (hex.length() == 1) {
			hex = '0' + hex;
		}
		return "0x" + hex;
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.activity_main, menu);
		return true;
	}

}
