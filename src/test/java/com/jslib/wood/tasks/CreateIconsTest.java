package com.jslib.wood.tasks;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.jslib.api.json.Json;
import com.jslib.docore.IFiles;
import com.jslib.dospi.IParameters;
import com.jslib.dospi.IShell;
import com.jslib.dospi.TaskAbortException;
import com.jslib.tools.imagick.ConvertProcess;
import com.jslib.wood.tasks.util.TaskContext;

@RunWith(MockitoJUnitRunner.class)
public class CreateIconsTest {
	@Mock
	private TaskContext context;

	@Mock
	private IShell shell;
	@Mock
	private IFiles files;
	@Mock
	private Json json;
	@Mock
	private ConvertProcess convert;

	@Mock
	private IParameters parameters;

	private CreateIcons task;

	@Before
	public void beforeTest() throws TaskAbortException {
		task = new CreateIcons(context, shell, files, json, convert);
	}

	@Test
	public void Given_WhenExecute_Then() throws Exception {
		// given
		when(context.getex("imagick.convert.path")).thenReturn("C://Program Files/ImageMagick-7.1.0-Q16/convert.bat");

		// when
		String backgroundColor = "#FF0000";
		File backgroundFile = new File("background.png");
		task.imagick("-size 512x512 xc:none -fill ${color} -draw \"circle 256,256 256,1\" ${file}", backgroundColor, backgroundFile);

		// then
		ArgumentCaptor<String> commandArg = ArgumentCaptor.forClass(String.class);
		verify(convert, times(1)).exec(commandArg.capture());
		assertThat(commandArg.getValue(), equalTo("-size 512x512 xc:none -fill #FF0000 -draw \"circle 256,256 256,1\" \"background.png\""));
	}
}
