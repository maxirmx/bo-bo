({
	baseUrl : "${project.build.directory}/${project.build.finalName}/javascript",
	dir : "${project.build.directory}/require-build/javascript/",
	optimize : "none",
	paths : {
		requireLib : 'require'
	},
	namespace : 'webswingRequirejs',
	modules : [ {
		name : "webswing-embed",
		include : [ 'requireLib', 'main', 'jquery-private'],
		create : true
	} ],
	wrap: {
		start: "/*! Webswing version ${project.version} (${git.commit.id.describe})*/ \n try{",
		endFile: "${project.basedir}/src/main/config/parts/end.frag"
	}
})