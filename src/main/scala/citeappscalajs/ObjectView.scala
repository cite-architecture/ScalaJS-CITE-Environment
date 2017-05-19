package citeapp

import com.thoughtworks.binding.{Binding, dom}
import com.thoughtworks.binding.Binding.{BindingSeq, Var, Vars}
import scala.scalajs.js
import scala.scalajs.js._
import org.scalajs.dom._
import org.scalajs.dom.ext._
import org.scalajs.dom.raw._
import edu.holycross.shot.cite._
import edu.holycross.shot.ohco2._
import edu.holycross.shot.citeobj._
import scala.scalajs.js.Dynamic.{ global => g }
import scala.scalajs.js.annotation.JSExport

@JSExport
object ObjectView {


	// HTML Div holding messages
	@dom
	def objectMessageDiv = {
		<div id="object_message" class={ s"app_message ${ObjectModel.userMessageVisibility.bind} ${ObjectModel.userAlert.bind}"  }>
		<p>{ ObjectModel.userMessage.bind }  </p>
		</div>
	}


	// HTML Div: main div for object display
	@dom
	def objectDiv = {
		val urnValidatingKeyUpHandler = { event: KeyboardEvent =>
			(event.currentTarget, event.keyCode) match {
				case (input: html.Input, KeyCode.Enter) => {
					event.preventDefault()
					ObjectController.changeUrn(s"${input.value.toString}")
					//input.value = ""
				}
				case(input: html.Input, _) =>  ObjectController.validateUrn(s"${input.value.toString}")
				case _ =>
			}
		}

		<div id="object_Container">

		<div id="object_sidebar" class="app_sidebarDiv">
		{ objectCollectionsContainer.bind }
		</div>

		{ objectMessageDiv.bind }

		<p id="object_reportingCurrentUrn" class="app_reportingCurrentUrn"> { ObjectModel.urn.bind.toString } </p>


		<p id="object_urnInputP">
		<input
		class={ s"object_inputFor_${ObjectModel.objectOrCollection.bind}" }
		id="object_urnInput"
		size={ 40 }
		type="text"
		value={ ObjectModel.urn.bind.toString }
		onkeyup={ urnValidatingKeyUpHandler }>
		</input>

	{ ObjectView.retrieveObjectButton.bind }
	{ ObjectView.objectToCollectionButton.bind }

	{ collectionBrowseControls.bind }

	</p>

	{ objectContainer.bind }

	</div>
}

@dom
def retrieveObjectButton = {
	<button
			onclick={ event: Event => {
				val s:String = js.Dynamic.global.document.getElementById("object_urnInput").value.toString
				ObjectModel.urn := Cite2Urn(s)
				ObjectController.updateUserMessage("Retrieving object…",1)
				js.timers.setTimeout(500){ ObjectController.changeObject }
				}
			}
			disabled={
						(ObjectModel.objectOrCollection.bind == "none")
					 }

> {

	ObjectModel.objectOrCollection.bind match {
		case "object" => {"Retrieve object"}
		case "collection" => {"Browse collection"}
		case "range" => {"Retrieve range"}
		case _ => {"Invalid URN"}
	}

}
</button>
}

@dom
def objectToCollectionButton = {
	<button
			onclick={ event: Event => {
				val s:String = js.Dynamic.global.document.getElementById("object_urnInput").value.toString
				ObjectModel.urn := Cite2Urn(s).dropSelector
				//ObjectModel.offset := 1
				//ObjectModel.limit := 2
				ObjectModel.objectOrCollection := "collection"
				ObjectController.updateUserMessage("Retrieving collection…",1)
				js.timers.setTimeout(500){ ObjectController.changeObject }
				}
			}
		class={
			ObjectModel.objectOrCollection.bind match {
				case "collection" => "app_hidden"
				case "range" => "app_visible"
				case "object" => "app_visible"
				case _ => "app_hidden"
			}
		}
			disabled={
						((ObjectModel.objectOrCollection.bind == "none") || (ObjectModel.objectOrCollection.bind == "collection"))
					 }

> Browse this Object’s Collection </button>
}

/* Passage Container */
@dom
def objectContainer = {
	<div id="object_objectContainer" data:bgtext="No Object"
	class={ s"""${if( ObjectModel.objects.bind.size == 0 ){ "object_empty" } else {"object_not_empty"}}""" }
	>

		<div id="object_navButtonContainer_top"
		class={ if(ObjectModel.browsable.bind){"app_visible"} else {"app_hidden"}}>
			{ prevButton.bind }
			{ nextButton.bind }
		</div>

	{ renderObjects.bind }

		<div id="object_navButtonContainer_bottom"
		class={ if(ObjectModel.browsable.bind){"app_visible"} else {"app_hidden"}}>
			{ prevButton.bind }
			{ nextButton.bind }
		</div>

	</div>
}

/* Fancy switcher, either listing objects as urn+label, or showing all the object's propeties. */

@dom
def renderObjects = {
	<ul>
	{
		for (obj <- ObjectModel.displayObjects ) yield {
			if ((ObjectModel.showObjects.get) || (ObjectModel.objectOrCollection.get == "object")){

				val collUrn = ObjectModel.urn.get.dropSelector
				val propList = obj.propertyList.map(pl => {

				val pt = ObjectModel.collections.get.filter(_.urn == collUrn)(0).propertyDefs.filter(_.urn == pl.urn.dropSelector)(0).propertyType.toString

					Var(Tuple3(pl.urn,pt,pl.propertyValue.toString))

				}).toList
				<li class="tables">
					<table>
						<tr>
	            <th>Property</th>
	            <th>Type</th>
	            <th>Value</th>
						</tr>

						{ renderList(propList).bind }

					</table>
				</li>
			} else {
			 <li class="list"><strong>
					{ obj.urn.toString }
					</strong>
				  -
					{ obj.label }
				</li>
			}
		}
	}
	</ul>
}

@dom def renderList(data: List[Var[Tuple3[Cite2Urn,String,String]]]) = {
  import scalaz.std.list._
			for (v <- data) yield {
				<tr>
		    <td>{v.bind._1.toString}</td>
		    <td>{v.bind._2.toString}</td>
		    <td>{v.bind._3.toString}</td>
				</tr>
		  }
}

/* Controls for limit and offset, as well as listing or showing objects */

@dom
def collectionBrowseControls = {
		<div id="object_browseControls"
		class={
			ObjectModel.objectOrCollection.bind match {
				case "collection" => "app_visible"
				case "range" => "app_visible"
				case _ => "app_hidden"
			}
		}

		>

			<label for="object_browseOffset">Start at</label>
			<input type="text" id="object_browseOffset" size={5} value={ObjectModel.offset.bind.toString}
			onchange={ event: Event => ObjectController.validateNumericEntry( event )}
			/>
			<label for="object_browseLimit">Show</label>
			<input type="text" id="object_browseLimit" size={3} value={ObjectModel.limit.bind.toString}
			onchange={ event: Event => ObjectController.validateNumericEntry( event )} />

			<div class="onoffswitch">
			    <input type="checkbox" name="onoffswitch" class="onoffswitch-checkbox" id="browse_onoffswitch" checked={false}
					onchange={ event: Event => js.timers.setTimeout(500){ ObjectController.switchDisplay( event )}}
					/>
			    <label class="onoffswitch-label" for="browse_onoffswitch">
			        <span class="onoffswitch-inner"></span>
			        <span class="onoffswitch-switch"></span>
			    </label>
			</div>
		</div>
}



/* Cited Works List */
@dom
def objectCollectionsContainer = {
	<div id="object_objectCollectionsContainer">
	<h2>CITE Collections</h2>
	<ul>
	{
		for (cc <- ObjectModel.collections) yield {
			<li>
			{ collectionUrnSpan( cc.urn ).bind } <br/>
			{ cc.collectionLabel }
			{ if(cc.isOrdered) "[ordered]" else "[unordered]" }
			<br/>
			{ ObjectModel.countObjects(cc.urn).toString } objects.

			</li>
		}
	}
	</ul>
	</div>
}



/* General-use functions for making clickable URNs */
@dom
def collectionUrnSpan(urn:Cite2Urn) = {
	<span
	class="app_clickable"
	onclick={ event: Event => {
		ObjectController.insertFirstObjectUrn(urn)
		ObjectModel.clearObject
		}
	}>
	{ urn.toString }
	</span>
}


	/* Navigation Buttons */
	@dom
	def nextButton = {
		<button
		class="navButton"
		onclick={ event: Event => ObjectController.getNext }
		disabled= {
			(ObjectModel.currentNext.bind == None)
		}
		> → </button>
	}

	@dom
	def prevButton = {
		<button
		class="navButton"
		onclick={ event: Event => ObjectController.getPrev }
		disabled= {
			(ObjectModel.currentPrev.bind == None)
		}
		> ← </button>
	}

}
