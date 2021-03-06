/*******************************************************************************
 * sdrtrunk
 * Copyright (C) 2014-2017 Dennis Sheirer
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 *
 ******************************************************************************/
package io.github.dsheirer.instrument.gui;

import io.github.dsheirer.alias.AliasModel;
import io.github.dsheirer.controller.channel.Channel;
import io.github.dsheirer.controller.channel.ChannelModel;
import io.github.dsheirer.controller.channel.ChannelProcessingManager;
import io.github.dsheirer.controller.channel.map.ChannelMapModel;
import io.github.dsheirer.instrument.Instrumentable;
import io.github.dsheirer.instrument.InstrumentableProcessingChain;
import io.github.dsheirer.instrument.tap.Tap;
import io.github.dsheirer.instrument.tap.TapGroup;
import io.github.dsheirer.instrument.tap.TapViewPanel;
import io.github.dsheirer.instrument.tap.stream.BinaryTap;
import io.github.dsheirer.instrument.tap.stream.BinaryTapViewPanel;
import io.github.dsheirer.instrument.tap.stream.ComplexSampleTap;
import io.github.dsheirer.instrument.tap.stream.ComplexSampleTapViewPanel;
import io.github.dsheirer.instrument.tap.stream.ComplexTap;
import io.github.dsheirer.instrument.tap.stream.ComplexTapViewPanel;
import io.github.dsheirer.instrument.tap.stream.DibitTap;
import io.github.dsheirer.instrument.tap.stream.DibitTapViewPanel;
import io.github.dsheirer.instrument.tap.stream.EyeDiagramDataTap;
import io.github.dsheirer.instrument.tap.stream.EyeDiagramDataTapPanel;
import io.github.dsheirer.instrument.tap.stream.FloatTap;
import io.github.dsheirer.instrument.tap.stream.FloatTapViewPanel;
import io.github.dsheirer.instrument.tap.stream.QPSKTap;
import io.github.dsheirer.instrument.tap.stream.QPSKTapPanel;
import io.github.dsheirer.instrument.tap.stream.SymbolEventTap;
import io.github.dsheirer.instrument.tap.stream.SymbolEventTapViewPanel;
import io.github.dsheirer.message.Message;
import io.github.dsheirer.module.Module;
import io.github.dsheirer.module.decode.DecoderFactory;
import io.github.dsheirer.module.decode.config.DecodeConfiguration;
import io.github.dsheirer.module.log.EventLogManager;
import io.github.dsheirer.playlist.PlaylistManager;
import io.github.dsheirer.sample.Listener;
import io.github.dsheirer.source.IControllableFileSource;
import io.github.dsheirer.source.Source;
import net.miginfocom.swing.MigLayout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.HashMap;
import java.util.List;

public class DecoderViewFrame extends JInternalFrame implements Listener<Message>
{
    private static final long serialVersionUID = 1L;
    private final static Logger mLog = LoggerFactory.getLogger(DecoderViewFrame.class);

    private DecodeConfiguration mDecodeConfig;
    private InstrumentableProcessingChain mProcessingChain;
    private IControllableFileSource mSource;

    private HashMap<Tap,TapViewPanel> mPanelMap = new HashMap<Tap,TapViewPanel>();

    public DecoderViewFrame(PlaylistManager playlistManager, Channel channel, IControllableFileSource source)
    {
        mSource = source;

        mDecodeConfig = channel.getDecodeConfiguration();

        mProcessingChain = new InstrumentableProcessingChain();

        ChannelModel channelModel = new ChannelModel();
        ChannelMapModel channelMapModel = new ChannelMapModel();

        ChannelProcessingManager channelProcessingManager = new ChannelProcessingManager(channelModel, channelMapModel,
            new AliasModel(), new EventLogManager(), null, null);

        List<Module> modules = DecoderFactory.getModules(channelModel, channelMapModel, channelProcessingManager,
            new AliasModel(), channel, mProcessingChain.getChannelState().getMutableMetadata());

        mProcessingChain.addModules(modules);

        mProcessingChain.addMessageListener(this);

        mProcessingChain.setSource((Source) mSource);

        mProcessingChain.start();

        initGui();
    }

    private void initGui()
    {
        setLayout(new MigLayout("insets 0 0 0 0 ", "[grow,fill]", "[grow,fill]"));

        setTitle("Decoder [" + mDecodeConfig.getDecoderType().getDisplayString() + "]");
        setPreferredSize(new Dimension(450, 250));
        setSize(450, 250);

        setResizable(true);
        setClosable(true);
        setIconifiable(true);
        setMaximizable(false);

        addMouseListener(new MouseListener()
        {
            @Override
            public void mouseClicked(MouseEvent me)
            {
                if(me.getButton() == MouseEvent.BUTTON3)
                {
                    JPopupMenu popup = new JPopupMenu();

                    popup.add(getTapContextMenu());

                    if(!mPanelMap.values().isEmpty())
                    {
                        popup.add(new JSeparator());

                        for(TapViewPanel panel : mPanelMap.values())
                        {
                            popup.add(panel.getContextMenu());
                        }
                    }

                    popup.show(DecoderViewFrame.this, me.getX(), me.getY());
                }
            }

            @Override
            public void mouseEntered(MouseEvent arg0)
            {
            }

            @Override
            public void mouseExited(MouseEvent arg0)
            {
            }

            @Override
            public void mousePressed(MouseEvent arg0)
            {
            }

            @Override
            public void mouseReleased(MouseEvent arg0)
            {
            }

        });
    }

    private JMenu getTapContextMenu()
    {
        JMenu menu = new JMenu("Modules");

        if(mProcessingChain != null)
        {
            for(Module module : mProcessingChain.getModules())
            {
                if(module instanceof Instrumentable)
                {
                    List<TapGroup> groups = ((Instrumentable) module).getTapGroups();

                    for(TapGroup group : groups)
                    {
                        menu.add(new TapGroupMenu(group));
                    }
                }
            }
        }

        return menu;
    }

    public void add(Tap tap)
    {
        TapViewPanel panel = null;

        switch(tap.getType())
        {
            case EVENT_SYNC_DETECT:
                break;
            case STREAM_BINARY:
                panel = new BinaryTapViewPanel((BinaryTap) tap);
                break;
            case STREAM_COMPLEX:
                panel = new ComplexTapViewPanel((ComplexTap) tap);
                break;
            case STREAM_COMPLEX_SAMPLE:
                panel = new ComplexSampleTapViewPanel((ComplexSampleTap) tap);
                break;
            case STREAM_DIBIT:
                panel = new DibitTapViewPanel((DibitTap) tap);
                break;
            case STREAM_EYE_DIAGRAM:
                panel = new EyeDiagramDataTapPanel((EyeDiagramDataTap) tap);
                break;
            case STREAM_FLOAT:
                panel = new FloatTapViewPanel((FloatTap) tap);
                break;
            case STREAM_QPSK:
                panel = new QPSKTapPanel((QPSKTap) tap);
                break;
            case STREAM_SYMBOL:
                panel = new SymbolEventTapViewPanel((SymbolEventTap) tap);
                break;
        }

        if(panel != null)
        {
            add(panel, "span");

            validate();

            mPanelMap.put(tap, panel);

            for(Module module : mProcessingChain.getModules())
            {
                if(module instanceof Instrumentable)
                {
                    ((Instrumentable) module).registerTap(tap);
                }
            }
        }
        else
        {
            mLog.info("Tap panel is null, couldn't add for tap " +
                tap.getName() + "[" + tap.getType().toString() + "]");
        }
    }

    public void remove(Tap tap)
    {
        for(Module module : mProcessingChain.getModules())
        {
            if(module instanceof Instrumentable)
            {
                ((Instrumentable) module).unregisterTap(tap);
            }
        }

        TapViewPanel panel = mPanelMap.get(tap);

        remove(panel);

        validate();

        mPanelMap.remove(tap);
    }

    public class AddAllTapsItem extends JMenuItem
    {
        private static final long serialVersionUID = 1L;

        private final List<Tap> mTaps;

        public AddAllTapsItem(final List<Tap> taps)
        {
            super("All Taps");

            mTaps = taps;

            addActionListener(new ActionListener()
            {
                @Override
                public void actionPerformed(ActionEvent e)
                {
                    for(Tap tap : mTaps)
                    {
                        DecoderViewFrame.this.add(tap);
                    }
                }
            });
        }
    }

    public class TapSelectionItem extends JCheckBoxMenuItem
    {
        private static final long serialVersionUID = 1L;
        private Tap mTap;

        public TapSelectionItem(Tap tap)
        {
            super(tap.getName());

            mTap = tap;

            if(mPanelMap.keySet().contains(tap))
            {
                setSelected(true);
            }

            addActionListener(new ActionListener()
            {
                @Override
                public void actionPerformed(ActionEvent arg0)
                {
                    if(mPanelMap.keySet().contains(mTap))
                    {
                        DecoderViewFrame.this.remove(mTap);
                    }
                    else
                    {
                        DecoderViewFrame.this.add(mTap);
                    }
                }
            });
        }
    }

    @Override
    public void receive(Message message)
    {
        mLog.info(message.toString());
    }

    public class TapGroupMenu extends JMenu
    {
        private static final long serialVersionUID = 1L;
        private TapGroup mTapGroup;

        public TapGroupMenu(TapGroup tapGroup)
        {
            super(tapGroup.getName());

            mTapGroup = tapGroup;

            add(new AddAllTapsItem(mTapGroup.getTaps()));

            add(new JSeparator());

            for(Tap tap : mTapGroup.getTaps())
            {
                add(new TapSelectionItem(tap));
            }
        }
    }
}
