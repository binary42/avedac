/*
 * @(#)IAction.java   10/03/17
 * 
 * Copyright 2010 MBARI
 *
 * Licensed under the GNU LESSER GENERAL PUBLIC LICENSE, Version 2.1
 * (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.gnu.org/copyleft/lesser.html
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */



package org.mbari.aved.ui.classifier.knowledgebase;

/**
 * <p>Interface for custom actions. It consists of a single method doAction()</p>
 *
 * @author     <a href="http://www.mbari.org">MBARI</a>
 * @created    October 3, 2004
 * @version    $Id: IAction.java,v 1.1 2010/02/03 21:21:53 dcline Exp $
 * @see        ActionAdapter
 */
public interface IAction {

    /**
     * <p><!-- Method description --></p>
     *
     */
    void doAction();
}
