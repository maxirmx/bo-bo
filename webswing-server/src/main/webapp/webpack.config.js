const path = require('path');
const webpack = require('webpack');
const HtmlWebpackPlugin = require('html-webpack-plugin');
const CleanWebpackPlugin = require('clean-webpack-plugin');

module.exports = {
  target:'web',  
  resolve: {
		extensions: ['.js'],
		modules: [path.resolve(__dirname, 'node_modules'),path.resolve(__dirname, 'node_modules/blueimp-file-upload/js/vendor')]
  },
  entry: './javascript/index.js',
  output: {
    filename: 'webswing-embed.js',
    path: path.resolve(__dirname, 'dist'),
	publicPath: '/'
  },
  optimization: {
    minimize: false,
	noEmitOnErrors: true,
	moduleIds: 'hashed'
  },
  devServer: {
    contentBase: './dist'
  },
  node: {
    fs: "empty"
  },
  stats: {
    colors: true
  },
  plugins: [
    new CleanWebpackPlugin(['dist']),
    new HtmlWebpackPlugin({
      title: 'Webswing'
    }),
    new webpack.DefinePlugin({
      'process.env.NODE_OPTIONS': JSON.stringify('--openssl-legacy-provider')
    })
  ],
  module: {
      rules: [       
		{
          test: /\.m?js$/,
          exclude: /node_modules/,
          use: {
            loader: 'babel-loader',
			options: {
			  presets: ['@babel/preset-env'],
              plugins: ['@babel/plugin-transform-runtime']
            }
		  }
        },
	    {
          test: /\.(html|css|proto)$/,
          use: [
            'raw-loader'
          ]
        },
        {
          test: /\.(png|svg|jpg|gif)$/,
          use: [
            'file-loader'
          ]
        }
      ]
  }
};