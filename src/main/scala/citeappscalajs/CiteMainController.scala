package citeapp
import com.thoughtworks.binding._
import scala.scalajs.js
import org.scalajs.dom.document
import org.scalajs.dom.raw._
import org.scalajs.dom.ext.Ajax
import scala.concurrent
.ExecutionContext
.Implicits
.global
import edu.holycross.shot.cite._
import edu.holycross.shot.scm._
import edu.holycross.shot.ohco2._
import scala.scalajs.js.annotation.JSExport

@JSExport
object CiteMainController {

	@JSExport
	def main(libUrl: String, libDelim: String): Unit = {

		CiteMainController.updateUserMessage("Loading default library. Please be patient…",1)
		js.timers.setTimeout(500){ CiteMainController.loadRemoteLibrary(libUrl, libDelim) }

		dom.render(document.body, CiteMainView.mainDiv)
	}

	def loadRemoteLibrary(url: String, libDelim: String):Unit = {

		val xhr = new XMLHttpRequest()
		xhr.open("GET", url )
		xhr.onload = { (e: Event) =>
			if (xhr.status == 200) {
				val contents:String = xhr.responseText
				CiteMainController.updateRepository(contents, libDelim)
			} else {
				CiteMainController.updateUserMessage(s"Request for remote library failed with code ${xhr.status}",2)
			}
		}
		xhr.send()

		/*
		Ajax.get(url).onSuccess { case xhr =>
		CiteMainController.updateUserMessage("Got remote library.",0)
		val contents:String = xhr.responseText
		CiteMainController.updateRepository(contents, libDelim)
	}
	*/
}

	def updateUserMessage(msg: String, alert: Int): Unit = {
		CiteMainModel.userMessageVisibility := "app_visible"
		CiteMainModel.userMessage := msg
		alert match {
			case 0 => CiteMainModel.userAlert := "default"
			case 1 => CiteMainModel.userAlert := "wait"
			case 2 => CiteMainModel.userAlert := "warn"
		}
		js.timers.clearTimeout(CiteMainModel.msgTimer)
		CiteMainModel.msgTimer = js.timers.setTimeout(20000){ CiteMainModel.userMessageVisibility := "app_hidden" }
	}


	def loadLocalLibrary(e: Event):Unit = {
		val reader = new org.scalajs.dom.raw.FileReader()
		val delimiter:String = {
			val delimiterChoice = js.Dynamic.global.document.getElementById("app_filePicker_delimiter").value.toString
			if (delimiterChoice == "TAB" ){ val d = "\t"; d } else { val d = "#"; d }
		}
		CiteMainController.updateUserMessage("Loading local library.",0)
		reader.readAsText(e.target.asInstanceOf[org.scalajs.dom.raw.HTMLInputElement].files(0))
		reader.onload = (e: Event) => {
			val contents = reader.result.asInstanceOf[String]
			CiteMainController.updateRepository(contents,delimiter)
		}
	}

	def retrieveTextPassage(urn:CtsUrn):Unit = {
			O2Controller.changeUrn(urn)
			js.Dynamic.global.document.getElementById("tab-1").checked = true
	}

	@dom
	def updateRepository(cexString: String, columnDelimiter: String = "\t") = {

		try {
			val repo:CiteLibrary = CiteLibrary(cexString, columnDelimiter)
			println("got here okay.")
			val mdString = s"Repository: ${repo.name}. Library URN: ${repo.urn}. License: ${repo.license}"

			repo.textRepository match {
				case Some(tr) => {
					CiteMainModel.currentLibraryMetadataString := mdString
					CiteMainController.updateUserMessage(s"Created new corpus. ${mdString}",0)
					O2Model.textRepository = tr
					CiteMainController.updateUserMessage(s"Updated text repository: ${ O2Model.textRepository.catalog.size } works.",0)

					O2Model.updateCitedWorks
					NGModel.updateCitedWorks
					NGController.clearResults
					NGController.clearHistory
					O2Model.clearPassage

					O2Controller.preloadUrn
					NGController.preloadUrn
				}
				case None => {
					CiteMainController.updateUserMessage("Chosen repository does not seem to include a TextRepository",2)
				}
			}

		} catch  {
			case e: Exception => {
				CiteMainController.updateUserMessage(s"""${e}. You might check to be sure you specified the correct delimiter (<tab> or "#").""",2)
			}
		}

	}


}
