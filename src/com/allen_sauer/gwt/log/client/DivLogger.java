/*
 * Copyright 2008 Fred Sauer
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.allen_sauer.gwt.log.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.DeferredCommand;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.WindowResizeListener;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.DockPanel;
import com.google.gwt.user.client.ui.FocusPanel;
import com.google.gwt.user.client.ui.FocusWidget;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.MouseListenerAdapter;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.Widget;

import com.allen_sauer.gwt.log.client.util.DOMUtil;
import com.allen_sauer.gwt.log.client.util.LogUtil;

/**
 * Logger which outputs to a draggable floating <code>DIV</code>.
 */
public class DivLogger extends AbstractLogger {
  // CHECKSTYLE_JAVADOC_OFF

  // TODO Add GWT.getVersion() after 1.5
  private static final String ABOUT_TEXT = "" + //
      "gwt-log-" + Log.getVersion() + " - Runtime logging for your Google Web Toolkit projects\n" + //
      "Copyright 2007-2008 Fred Sauer\n" + //
      "The original software is available from:\n" + //
      "\u00a0\u00a0\u00a0\u00a0http://allen-sauer.com/gwt/"; //

  private static final String CSS_LOG_MESSAGE = "log-message";
  private static final int[] levels = {
      Log.LOG_LEVEL_DEBUG, Log.LOG_LEVEL_INFO, Log.LOG_LEVEL_WARN, Log.LOG_LEVEL_ERROR,
      Log.LOG_LEVEL_FATAL, Log.LOG_LEVEL_OFF,};
  private static final String STACKTRACE_ELEMENT_PREFIX = "&nbsp;&nbsp;&nbsp;&nbsp;at&nbsp;";
  private static final String STYLE_LOG_HEADER = "log-header";
  private static final String STYLE_LOG_PANEL = "log-panel";
  private static final String STYLE_LOG_SCROLL_PANEL = "log-scroll-panel";
  private static final String STYLE_LOG_TEXT_AREA = "log-text-area";
  private static final int UPDATE_INTERVAL_MILLIS = 500;

  private DockPanel debugDockPanel = new DockPanel() {
    private WindowResizeListener windowResizeListener = new WindowResizeListener() {
      public void onWindowResized(int width, int height) {
        debugDockPanel.setPixelSize(Math.max(300, (int) (Window.getClientWidth() * .8)), Math.max(
            100, (int) (Window.getClientHeight() * .3)));
      }
    };

    protected void onLoad() {
      super.onLoad();
      windowResizeListener.onWindowResized(Window.getClientWidth(), Window.getClientHeight());
      Window.addWindowResizeListener(windowResizeListener);
    }

    protected void onUnload() {
      super.onUnload();
      Window.removeWindowResizeListener(windowResizeListener);
    }
  };
  private boolean dirty = false;
  private Button[] levelButtons;
  private String logText = "";
  private HTML logTextArea = new HTML();

  private ScrollPanel scrollPanel = new ScrollPanel();

  private Timer timer;

  /**
   * Default constructor.
   */
  public DivLogger() {
    debugDockPanel.addStyleName(STYLE_LOG_PANEL);
    logTextArea.addStyleName(STYLE_LOG_TEXT_AREA);
    scrollPanel.addStyleName(STYLE_LOG_SCROLL_PANEL);

    final FocusPanel headerPanel = makeHeader();

    Widget resizePanel;
    resizePanel = makeResizePanel();

    debugDockPanel.add(headerPanel, DockPanel.NORTH);
    debugDockPanel.add(scrollPanel, DockPanel.CENTER);
    debugDockPanel.add(resizePanel, DockPanel.SOUTH);
    debugDockPanel.setCellWidth(scrollPanel, "100%");
    debugDockPanel.setCellHeight(scrollPanel, "100%");
    debugDockPanel.setCellHorizontalAlignment(resizePanel, HasHorizontalAlignment.ALIGN_RIGHT);

    scrollPanel.setWidget(logTextArea);

    debugDockPanel.setVisible(false);
    RootPanel.get().add(debugDockPanel, 0, 0);

    timer = new Timer() {
      public void run() {
        dirty = false;
        logTextArea.setHTML(logTextArea.getHTML() + logText);
        logText = "";
        DeferredCommand.addCommand(new Command() {
          public void execute() {
            scrollPanel.setScrollPosition(Integer.MAX_VALUE);
          }
        });
      }
    };
  }

  public final void clear() {
    logTextArea.setHTML("");
  }

  public final Widget getWidget() {
    return debugDockPanel;
  }

  public final boolean isSupported() {
    return true;
  }

  public final boolean isVisible() {
    return debugDockPanel.isAttached() && debugDockPanel.isVisible();
  }

  public final void moveTo(int x, int y) {
    RootPanel.get().add(debugDockPanel, x, y);
  }

  public void setCurrentLogLevel(int level) {
    super.setCurrentLogLevel(level);
    for (int i = 0; i < levels.length; i++) {
      if (levels[i] < Log.getLowestLogLevel()) {
        levelButtons[i].setEnabled(false);
      } else {
        String levelText = LogUtil.levelToString(levels[i]);
        boolean current = level == levels[i];
        levelButtons[i].setTitle(current ? "Current (runtime) log level is already '" + levelText
            + "'" : "Set current (runtime) log level to '" + levelText + "'");
        boolean active = level <= levels[i];
        DOM.setStyleAttribute(levelButtons[i].getElement(), "color", active ? getColor(levels[i])
            : "#ccc");
      }
    }
  }

  public final void setPixelSize(int width, int height) {
    logTextArea.setPixelSize(width, height);
  }

  public final void setSize(String width, String height) {
    logTextArea.setSize(width, height);
  }

  final void log(int logLevel, String message) {
    assert false;
    // Method never called since {@link #log(int, String, Throwable)} is overridden
  }

  final void log(int logLevel, String message, Throwable throwable) {
    String text = message.replaceAll("<", "&lt;").replaceAll(">", "&gt;");
    String title = makeTitle(message, throwable);
    if (throwable != null) {
      text += "\n";
      while (throwable != null) {
        text += GWT.getTypeName(throwable) + ":<br><b>" + throwable.getMessage() + "</b>";
        StackTraceElement[] stackTraceElements = throwable.getStackTrace();
        if (stackTraceElements.length > 0) {
          text += "<div class='log-stacktrace'>";
          for (int i = 0; i < stackTraceElements.length; i++) {
            text += STACKTRACE_ELEMENT_PREFIX + stackTraceElements[i] + "<br>";
          }
          text += "</div>";
        }
        throwable = throwable.getCause();
        if (throwable != null) {
          text += "Caused by: ";
        }
      }
    }
    text = text.replaceAll("\r\n|\r|\n", "<BR>");
    addLogText("<div class='" + CSS_LOG_MESSAGE
        + "' onmouseover='className+=\" log-message-hover\"' "
        + "onmouseout='className=className.replace(/ log-message-hover/g,\"\")' style='color: "
        + getColor(logLevel) + "' title='" + title + "'>" + text + "</div>");
    debugDockPanel.setVisible(true);
  }

  private void addLogText(String debugText) {
    logText += debugText;
    if (!dirty) {
      dirty = true;
      timer.schedule(UPDATE_INTERVAL_MILLIS);
    }
  }

  private String getColor(int logLevel) {
    if (logLevel == Log.LOG_LEVEL_OFF) {
      return "#000"; // black
    }
    if (logLevel >= Log.LOG_LEVEL_FATAL) {
      return "#F00"; // bright red
    }
    if (logLevel >= Log.LOG_LEVEL_ERROR) {
      return "#C11B17"; // dark red
    }
    if (logLevel >= Log.LOG_LEVEL_WARN) {
      return "#E56717"; // dark orange
    }
    if (logLevel >= Log.LOG_LEVEL_INFO) {
      return "#2B60DE"; // blue
    }
    return "#20b000"; // green
  }

  /**
   * @deprecated
   */
  private FocusPanel makeHeader() {
    FocusPanel header;
    header = new FocusPanel();
    HorizontalPanel masterPanel = new HorizontalPanel();
    masterPanel.setWidth("100%");
    header.add(masterPanel);
    header.addStyleName(STYLE_LOG_HEADER);

    final Label titleLabel = new Label("gwt-log", false);
    titleLabel.setStylePrimaryName("log-title");

    HorizontalPanel buttonPanel = new HorizontalPanel();
    levelButtons = new Button[levels.length];
    for (int i = 0; i < levels.length; i++) {
      final int level = levels[i];
      levelButtons[i] = new Button(LogUtil.levelToString(level));
      buttonPanel.add(levelButtons[i]);
      levelButtons[i].addClickListener(new ClickListener() {
        public void onClick(Widget sender) {
          ((FocusWidget) sender).setFocus(false);
          Log.setCurrentLogLevel(level);
        }
      });
    }

    Button clearButton = new Button("Clear");
    clearButton.addStyleName("log-clear-button");
    DOM.setStyleAttribute(clearButton.getElement(), "color", "#00c");
    clearButton.addClickListener(new ClickListener() {
      public void onClick(Widget sender) {
        ((FocusWidget) sender).setFocus(false);
        Log.clear();
      }
    });
    buttonPanel.add(clearButton);

    Button aboutButton = new Button("About");
    aboutButton.addStyleName("log-clear-about");
    aboutButton.addClickListener(new ClickListener() {
      public void onClick(Widget sender) {
        ((FocusWidget) sender).setFocus(false);
        Log.diagnostic(ABOUT_TEXT, null);
      }
    });

    masterPanel.add(titleLabel);
    masterPanel.add(buttonPanel);
    masterPanel.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_RIGHT);
    masterPanel.add(aboutButton);

    masterPanel.setCellHeight(titleLabel, "100%");
    masterPanel.setCellWidth(titleLabel, "50%");
    masterPanel.setCellWidth(aboutButton, "50%");

    titleLabel.addMouseListener(new MouseListenerAdapter() {
      private boolean dragging = false;
      private int dragStartX;
      private int dragStartY;

      public void onMouseDown(Widget sender, int x, int y) {
        dragging = true;
        DOM.setCapture(titleLabel.getElement());
        dragStartX = x;
        dragStartY = y;
      }

      public void onMouseMove(Widget sender, int x, int y) {
        if (dragging) {
          int absX = x + debugDockPanel.getAbsoluteLeft();
          int absY = y + debugDockPanel.getAbsoluteTop();
          RootPanel.get().setWidgetPosition(debugDockPanel, absX - dragStartX, absY - dragStartY);
        }
      }

      public void onMouseUp(Widget sender, int x, int y) {
        dragging = false;
        DOM.releaseCapture(titleLabel.getElement());
      }
    });

    return header;
  }

  private Widget makeResizePanel() {
    final Image handle = new Image(GWT.getModuleBaseURL() + "gwt-log-triangle-10x10.png");
    handle.addStyleName("log-resize-se");
    handle.addMouseListener(new MouseListenerAdapter() {
      private boolean dragging = false;
      private int dragStartX;
      private int dragStartY;

      public void onMouseDown(Widget sender, int x, int y) {
        dragging = true;
        DOM.setCapture(handle.getElement());
        dragStartX = x;
        dragStartY = y;
      }

      public void onMouseMove(Widget sender, int x, int y) {
        if (dragging) {
          int absX = x + handle.getAbsoluteLeft();
          int absY = y + handle.getAbsoluteTop();
          debugDockPanel.setPixelSize(absX - dragStartX, absY - dragStartY);
          scrollPanel.setScrollPosition(Integer.MAX_VALUE);
        }
      }

      public void onMouseUp(Widget sender, int x, int y) {
        dragging = false;
        DOM.releaseCapture(handle.getElement());
      }
    });
    return handle;
  }

  private String makeTitle(String message, Throwable throwable) {
    if (throwable != null) {
      if (throwable.getMessage() == null) {
        message = GWT.getTypeName(throwable);
      } else {
        message = throwable.getMessage().replaceAll(
            GWT.getTypeName(throwable).replaceAll("^(.+\\.).+$", "$1"), "");
      }
    }
    return DOMUtil.adjustTitleLineBreaks(message).replaceAll("<", "&lt;").replaceAll(">", "&gt;").replaceAll(
        "'", "\"");
  }
}